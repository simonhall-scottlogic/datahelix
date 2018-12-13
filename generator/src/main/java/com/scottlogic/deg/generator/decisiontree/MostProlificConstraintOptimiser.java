package com.scottlogic.deg.generator.decisiontree;

import com.scottlogic.deg.generator.constraints.atomic.AtomicConstraint;
import com.scottlogic.deg.generator.constraints.atomic.NotConstraint;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MostProlificConstraintOptimiser implements DecisionTreeOptimiser {
    private final int maxIterations;
    private final int maxDepth;

    public MostProlificConstraintOptimiser() {
        this(50, 10000000);
    }

    public MostProlificConstraintOptimiser(int maxIterations, int maxDepth) {
        this.maxIterations = maxIterations;
        this.maxDepth = maxDepth;
    }

    @Override
    public DecisionTree optimiseTree(DecisionTree tree){
        ConstraintNode newRootNode = optimiseLevelOfTree(tree.getRootNode(), 1);

        if (newRootNode == null)
            return tree;

        return new DecisionTree(newRootNode, tree.getFields(), tree.getDescription());
    }

    private ConstraintNode optimiseLevelOfTree(ConstraintNode rootNode, int depth){
        Collection<DecisionNode> decisions = rootNode.getDecisions();
        if (decisions.size() <= 1 || depth > this.maxDepth)
            return null; //not worth optimising

        int iteration = 0;
        int prevDecisionCount = decisions.size();
        ConstraintNode newRootNode;
        while (iteration < this.maxIterations && (newRootNode = optimiseDecisions(rootNode, depth)) != null)
        {
            rootNode = newRootNode;

            int newDecisionCount = rootNode.getDecisions().size();
            int changeInDecisionCount = newDecisionCount - prevDecisionCount;
            if (Math.abs(changeInDecisionCount) < 1) {
                break;
            }

            prevDecisionCount = newDecisionCount;
            iteration++;
        }

        return rootNode;
    }

    private ConstraintNode optimiseDecisions(ConstraintNode rootNode, int depth){
        AtomicConstraint mostProlificAtomicConstraint = getMostProlificAtomicConstraint(rootNode.getDecisions());
        if (mostProlificAtomicConstraint == null){
            return null;
        }
        // Add negation of most prolific constraint to new decision node
        AtomicConstraint negatedMostProlificConstraint = mostProlificAtomicConstraint.negate();

        List<DecisionNode> factorisableDecisionNodes = rootNode.getDecisions().stream()
            .filter(node -> this.decisionIsFactorisable(node, mostProlificAtomicConstraint, negatedMostProlificConstraint))
            .collect(Collectors.toList());
        if (factorisableDecisionNodes.size() < 2){
            return null;
        }

        // Add most prolific constraint to new decision node
        ConstraintNode factorisingConstraintNode = new TreeConstraintNode(mostProlificAtomicConstraint).markNode(NodeMarking.OPTIMISED);
        ConstraintNode negatedFactorisingConstraintNode = new TreeConstraintNode(negatedMostProlificConstraint).markNode(NodeMarking.OPTIMISED);;

        Set<ConstraintNode> otherOptions = new HashSet<>();
        Set<DecisionNode> decisionsToRemove = new HashSet<>();

        for (DecisionNode decision : factorisableDecisionNodes) {
            DecisionAnalyser analyser = new DecisionAnalyser(decision, mostProlificAtomicConstraint);
            DecisionAnalysisResult result = analyser.performAnalysis();

            // Perform movement of options
            factorisingConstraintNode = addOptionsAsDecisionUnderConstraintNode(factorisingConstraintNode, result.optionsToFactorise);
            negatedFactorisingConstraintNode = addOptionsAsDecisionUnderConstraintNode(negatedFactorisingConstraintNode, result.negatedOptionsToFactorise);
            otherOptions.addAll(result.adjacentOptions);
            decisionsToRemove.add(decision);
        }

        // Add new decision node
        DecisionNode factorisedDecisionNode = new TreeDecisionNode(
            Stream.concat(
                Stream.of(
                    coalesce(optimiseLevelOfTree(factorisingConstraintNode, depth + 1), factorisingConstraintNode),
                    coalesce(optimiseLevelOfTree(negatedFactorisingConstraintNode, depth + 1), negatedFactorisingConstraintNode)),
                otherOptions.stream())
            .collect(Collectors.toList())).markNode(NodeMarking.OPTIMISED);

        return rootNode
            .removeDecisions(decisionsToRemove)
            .addDecisions(Collections.singletonList(factorisedDecisionNode));
    }

    private boolean constraintNodeContainsNegatedConstraints(ConstraintNode node, Set<AtomicConstraint> constraints){
        return node.getAtomicConstraints().stream()
            .map(AtomicConstraint::negate)
            .allMatch(constraints::contains);
    }

    private ConstraintNode addOptionsAsDecisionUnderConstraintNode(
        ConstraintNode newNode,
        Collection<ConstraintNode> optionsToAdd) {
        if (optionsToAdd.isEmpty()) {
            return newNode;
        }

        DecisionNode decisionUnderFactorisedNode = new TreeDecisionNode(optionsToAdd).markNode(NodeMarking.OPTIMISED);
        return newNode.addDecisions(Collections.singletonList(decisionUnderFactorisedNode));
    }

    private int disfavourNotConstraints(Map.Entry<AtomicConstraint, Long> entry){
        return entry.getKey() instanceof NotConstraint ? 1 : 0;
    }

    private AtomicConstraint getMostProlificAtomicConstraint(Collection<DecisionNode> decisions) {
        Map<AtomicConstraint, Long> decisionConstraints = decisions
            .stream()
            .flatMap(dn -> dn.getOptions().stream())
            .flatMap(option -> option.getAtomicConstraints().stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Comparator<Map.Entry<AtomicConstraint, Long>> comparator = Comparator.comparing(Map.Entry::getValue);
        comparator = comparator.reversed();
        comparator = comparator.thenComparing(this::disfavourNotConstraints);
        comparator = comparator.thenComparing(entry -> entry.getKey().toString());

        return decisionConstraints.entrySet()
            .stream()
            .filter(constraint -> constraint.getValue() > 1) // where the number of occurrences > 1
            .sorted(comparator)
            .map(Map.Entry::getKey) //get a reference to the first identified atomic-constraint
            .findFirst()
            .orElse(null); //otherwise return null
    }

    private boolean decisionIsFactorisable(DecisionNode decision, AtomicConstraint factorisingConstraint, AtomicConstraint negatedFactorisingConstraint){
        // The decision should contain ONE option with the MPC
        boolean optionWithMPCExists = decision.getOptions().stream()
            .filter(option -> option.atomicConstraintExists(factorisingConstraint))
            .count() == 1;

        // The decision should contain ONE separate option with the negated MPC (which is atomic).
        boolean optionWithNegatedMPCExists = decision.getOptions().stream()
            .filter(option -> option.atomicConstraintExists(negatedFactorisingConstraint) && option.getAtomicConstraints().size() == 1)
            .count() == 1;

        return optionWithMPCExists && optionWithNegatedMPCExists;
    }

    private static <T> T coalesce(T... items){
        for (T item : items) {
            if (item != null)
                return item;
        }

        throw new UnsupportedOperationException("Unable to find a non-null value");
    }

    class DecisionAnalyser {
        private DecisionNode decision;
        private AtomicConstraint factorisingConstraint;
        private AtomicConstraint negatedFactorisingConstraint;
        private Set<AtomicConstraint> atomicConstraintsAssociatedWithFactorisingOption = new HashSet<>();
        private Set<AtomicConstraint> atomicConstraintsAssociatedWithNegatedOption = new HashSet<>();

        DecisionAnalyser(DecisionNode decisionNode, AtomicConstraint factorisingConstraint){
            this.decision = decisionNode;
            this.factorisingConstraint = factorisingConstraint;
            this.negatedFactorisingConstraint = factorisingConstraint.negate();
        }

        /**
         * Iterate through a decision nodes options and determine whether factorisation is possible
         */
        DecisionAnalysisResult performAnalysis() {
            DecisionAnalysisResult result = new DecisionAnalysisResult();
            List<ConstraintNode> otherOptions = new ArrayList<>();
            for (ConstraintNode option : decision.getOptions()) {
                boolean optionContainsProlificConstraint = option.atomicConstraintExists(factorisingConstraint);
                boolean optionContainsNegatedProlificConstraint = option.atomicConstraintExists(negatedFactorisingConstraint);
                if (optionContainsProlificConstraint && optionContainsNegatedProlificConstraint) {
                    throw new RuntimeException("Contradictory constraint node");
                } else if (optionContainsProlificConstraint) {
                    markOptionForFactorisation(factorisingConstraint, option, result.optionsToFactorise, atomicConstraintsAssociatedWithFactorisingOption);
                } else if (optionContainsNegatedProlificConstraint) {
                    markOptionForFactorisation(negatedFactorisingConstraint, option, result.negatedOptionsToFactorise, atomicConstraintsAssociatedWithNegatedOption);
                } else {
                    // This option does not contain the factorising constraint so add to a separate list.
                    otherOptions.add(option);
                }
            }

            // The following options need moving either to:
            // * an option under the factorising constraint node,
            // * an option under the negated factorising constraint node,
            // * or another option alongside the factorising constraint node
            for (ConstraintNode option : otherOptions) {
                boolean nodeCanBeMovedUnderFactorised = constraintNodeContainsNegatedConstraints(option, atomicConstraintsAssociatedWithFactorisingOption);
                boolean nodeCanBeMovedUnderNegatedFactorised = constraintNodeContainsNegatedConstraints(option, atomicConstraintsAssociatedWithNegatedOption);
                if (nodeCanBeMovedUnderFactorised) {
                    result.optionsToFactorise.add(option);
                } else if (nodeCanBeMovedUnderNegatedFactorised) {
                    result.negatedOptionsToFactorise.add(option);
                } else {
                    result.adjacentOptions.add(option);
                }
            }
            return result;
        }

        private void markOptionForFactorisation(AtomicConstraint factorisingConstraint, ConstraintNode node, List<ConstraintNode> options, Set<AtomicConstraint> constraints){
            ConstraintNode newOption = node.cloneWithoutAtomicConstraint(factorisingConstraint);
            if (!newOption.getAtomicConstraints().isEmpty()){
                options.add(newOption);
                constraints.addAll(newOption.getAtomicConstraints());
            }
        }
    }

    class DecisionAnalysisResult {
        List<ConstraintNode> optionsToFactorise = new ArrayList<>();
        List<ConstraintNode> negatedOptionsToFactorise = new ArrayList<>();
        List<ConstraintNode> adjacentOptions = new ArrayList<>();
    }
}
