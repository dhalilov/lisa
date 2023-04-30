package lisa.mathematics

import lisa.automation.grouptheory.GroupTheoryTactics.{Cases, Definition, ExistenceAndUniqueness}
import lisa.automation.kernel.OLPropositionalSolver.Tautology
import lisa.mathematics.FirstOrderLogic.{equalityTransitivity, existsOneImpliesExists, substitutionInUniquenessQuantifier}
import lisa.mathematics.SetTheory.*

/**
 * Group theory, following Chapter 2 of S. Lang "Undergraduate Algebra".
 *
 * Book : [[https://link.springer.com/book/10.1007/978-1-4684-9234-7]]
 */
object GroupTheory extends lisa.Main {
  // Groups
  private val G, H = variable

  // Group laws
  private val * = variable

  // Group elements
  private val x, y, z = variable
  private val u, v, w = variable

  // Identity elements
  private val e, f = variable

  // Predicates
  private val P, Q = predicate(1)

  //
  // 0. Notation
  //

  /**
   * Defines the element that is uniquely given by the uniqueness theorem, or falls back to the error element if the
   * assumptions of the theorem are not satisfied.
   *
   * This is useful in defining specific elements in groups, where their uniqueness (and existence) strongly rely
   * on the assumption of the group structure.
   *
   * TODO This should probably be merged with [[The]] with an additional `orElse` method, to be discussed
   */
  def TheConditional(u: VariableLabel, f: Formula)(just: runningSetTheory.Theorem, defaultValue: Term = ∅): The = {
    val seq = just.proposition

    if (seq.left.isEmpty) {
      The(u, f)(just)
    } else {
      val prem = if (seq.left.size == 1) seq.left.head else And(seq.left.toSeq: _*)
      val completeDef = (prem ==> f) /\ (!prem ==> (u === defaultValue))
      val substF = substituteVariables(completeDef, Map[VariableLabel, Term](u -> defaultValue))
      val substDef = substituteVariables(completeDef, Map[VariableLabel, Term](u -> v))

      val completeUniquenessTheorem = Lemma(
        ∃!(u, completeDef)
      ) {
        val case1 = have(prem |- ∃!(u, completeDef)) subproof {
          // We prove the equivalence f <=> completeDef so that we can substitute it in the uniqueness quantifier
          val equiv = have(prem |- ∀(u, f <=> completeDef)) subproof {
            have(f |- f) by Hypothesis
            thenHave((prem, f) |- f) by Weakening
            val left = thenHave(f |- (prem ==> f)) by Restate

            have(prem |- prem) by Hypothesis
            thenHave((prem, !prem) |- ()) by LeftNot
            thenHave((prem, !prem) |- (u === defaultValue)) by Weakening
            val right = thenHave(prem |- (!prem ==> (u === defaultValue))) by Restate

            have((prem, f) |- completeDef) by RightAnd(left, right)
            val forward = thenHave(prem |- f ==> completeDef) by Restate

            have(completeDef |- completeDef) by Hypothesis
            thenHave((prem, completeDef) |- completeDef) by Weakening
            thenHave((prem, completeDef) |- f) by Tautology
            val backward = thenHave(prem |- completeDef ==> f) by Restate

            have(prem |- f <=> completeDef) by RightIff(forward, backward)
            thenHave(thesis) by RightForall
          }

          val substitution = have((∃!(u, f), ∀(u, f <=> completeDef)) |- ∃!(u, completeDef)) by Restate.from(
            substitutionInUniquenessQuantifier of(P -> lambda(u, f), Q -> lambda(u, completeDef))
          )

          val implication = have((prem, ∃!(u, f)) |- ∃!(u, completeDef)) by Cut(equiv, substitution)
          val uniqueness = have(prem |- ∃!(u, f)) by Restate.from(just)
          have(prem |- ∃!(u, completeDef)) by Cut(uniqueness, implication)
        }

        val case2 = have(!prem |- ∃!(u, completeDef)) subproof {
          val existence = have(!prem |- ∃(u, completeDef)) subproof {
            have(!prem |- !prem) by Hypothesis
            thenHave((prem, !prem) |- ()) by LeftNot
            thenHave((prem, !prem) |- substF) by Weakening
            val left = thenHave(!prem |- (prem ==> substF)) by Restate

            have(defaultValue === defaultValue) by RightRefl
            thenHave(!prem |- defaultValue === defaultValue) by Weakening
            val right = thenHave(!prem ==> (defaultValue === defaultValue)) by Restate

            have(!prem |- (prem ==> substF) /\ (!prem ==> (defaultValue === defaultValue))) by RightAnd(left, right)
            thenHave(thesis) by RightExists.withParameters(defaultValue)
          }

          val uniqueness = have((!prem, completeDef, substDef) |- (u === v)) subproof {
            assume(!prem)
            assume(completeDef)
            assume(substDef)

            val eq1 = have(u === defaultValue) by Tautology
            val eq2 = have(defaultValue === v) by Tautology
            val p = have((u === defaultValue) /\ (defaultValue === v)) by RightAnd(eq1, eq2)

            val transitivity = equalityTransitivity of (x -> u, y -> defaultValue, z -> v)
            have(thesis) by Cut(p, transitivity)
          }

          have(thesis) by ExistenceAndUniqueness(completeDef)(existence, uniqueness)
        }

        have(thesis) by Cases(case1, case2)
      }

      The(u, completeDef)(completeUniquenessTheorem)
    }
  }

  //
  // 1. Basic definitions and results
  //

  /**
   * Binary function --- `*` is a binary function on `G` if it associates to each pair of elements of `G`
   * a unique element in `G`. In other words, `*` is a function `G × G -> G`.
   */
  val binaryFunction = DEF(G, *) --> functionFrom(*, cartesianProduct(G, G), G)

  /**
   * Short-hand alias for `x * y`.
   */
  inline def op(x: Term, * : Term, y: Term) = app(*, pair(x, y))

  /**
   * Associativity --- `*` is associative (in `G`) if `(x * y) * z = x * (y * z)` for all `x, y, z` in `G`.
   */
  val associativity = DEF(G, *) -->
    ∀(x, x ∈ G ==> ∀(y, y ∈ G ==> ∀(z, z ∈ G ==> (op(op(x, *, y), *, z) === op(x, *, op(y, *, z))))))

  /**
   * Neutral element --- We say that an element `e` in `G` is neutral if `e * x = x * e = x` for all `x` in `G`.
   */
  val isNeutral = DEF(e, G, *) --> (e ∈ G /\ ∀(x, (x ∈ G) ==> ((op(e, *, x) === x) /\ (op(x, *, e) === x))))

  /**
   * Identity existence --- There exists a neutral element `e` in `G`.
   */
  val identityExistence = DEF(G, *) --> ∃(e, isNeutral(e, G, *))

  /**
   * Inverse element --- `y` is called an inverse of `x` if `x * y = y * x = e`.
   */
  val isInverse = DEF(y, x, G, *) --> (y ∈ G) /\ isNeutral(op(x, *, y), G, *) /\ isNeutral(op(y, *, x), G, *)

  /**
   * Inverse existence --- For all `x` in G, there exists an element `y` in G such that `x * y = y * x = e`.
   */
  val inverseExistence = DEF(G, *) --> ∀(x, (x ∈ G) ==> ∃(y, isInverse(y, x, G, *)))

  /**
   * Group --- A group (G, *) is a set along with a law of composition `*`, satisfying [[associativity]], [[identityExistence]]
   * and [[inverseExistence]].
   */
  val group = DEF(G, *) --> binaryFunction(G, *) /\ associativity(G, *) /\ identityExistence(G, *) /\ inverseExistence(G, *)

  /**
   * Group operation is functional -- The group operation `*` is functional.
   *
   * Similarly to the lemma below, follows more or less directly from the definitions, but still useful.
   */
  val groupOperationIsFunctional = Lemma(
    group(G, *) |- functional(*)
  ) {
    assume(group(G, *))
    have(binaryFunction(G, *)) by Tautology.from(group.definition)
    have(functionFrom(*, cartesianProduct(G, G), G)) by Tautology.from(lastStep, binaryFunction.definition)
    have(functional(*)) by Tautology.from(lastStep, functionFromImpliesFunctional of (f -> *, x -> cartesianProduct(G, G), y -> G))
  }

  /**
   * Group operation domain -- The domain of a group law is the cartesian product of the group with itself.
   *
   * Follows directly from the definition of `binaryRelation`, but it is a useful lemma to have in some proofs.
   */
  val groupOperationDomain = Lemma(
    group(G, *) |- relationDomain(*) === cartesianProduct(G, G)
  ) {
    assume(group(G, *))
    have(binaryFunction(G, *)) by Tautology.from(group.definition)
    have(functionFrom(*, cartesianProduct(G, G), G)) by Tautology.from(lastStep, binaryFunction.definition)
    have(relationDomain(*) === cartesianProduct(G, G)) by Tautology.from(lastStep, functionFromImpliesDomainEq of(f -> *, x -> cartesianProduct(G, G), y -> G))
  }

  /**
   * Lemma --- If `x` and `y` are two elements of the group, the pair `(x, y)` is in the relation domain of `*.
   */
  val groupPairInOperationDomain = Lemma(
    (group(G, *), x ∈ G, y ∈ G) |- pair(x, y) ∈ relationDomain(*)
  ) {
    assume(group(G, *))
    assume(x ∈ G)
    assume(y ∈ G)

    val a, b = variable

    have(x ∈ G /\ y ∈ G) by Tautology
    have(pair(x, y) ∈ cartesianProduct(G, G)) by Tautology.from(
      lastStep, pairInCartesianProduct of (a -> x, b -> y, x -> G, y -> G)
    )
    thenHave((relationDomain(*) === cartesianProduct(G, G)) |- pair(x, y) ∈ relationDomain(*)) by RightSubstEq(
      List((relationDomain(*), cartesianProduct(G, G))), lambda(z, pair(x, y) ∈ z)
    )

    have(thesis) by Cut(groupOperationDomain, lastStep)
  }
  show

  /**
   * Identity uniqueness --- In a group (G, *), an identity element is unique, i.e. if both `e * x = x * e = x` and
   * `f * x = x * f = x` for all `x`, then `e = f`.
   * This justifies calling `e` <i>the</i> identity element.
   */
  val identityUniqueness = Theorem(
    group(G, *) |- ∃!(e, isNeutral(e, G, *))
  ) {
    val existence = have(group(G, *) |- ∃(e, isNeutral(e, G, *))) by Tautology.from(group.definition, identityExistence.definition)

    // We prove that if e and f are neutral elements then ef = f = e, where the first equality comes from e's left neutrality,
    // and the second equality from f's right neutrality
    val uniqueness = have((isNeutral(e, G, *), isNeutral(f, G, *)) |- (e === f)) subproof {
      // We prove that neutral elements are elements of G, such that * can be applied.
      val membership = have(isNeutral(e, G, *) |- e ∈ G) by Tautology.from(isNeutral.definition)

      assume(isNeutral(e, G, *))
      assume(isNeutral(f, G, *))

      // 1. ef = f
      have(∀(x, x ∈ G ==> ((op(e, *, x) === x) /\ (op(x, *, e) === x)))) by Tautology.from(isNeutral.definition)
      thenHave(f ∈ G ==> ((op(e, *, f) === f) /\ (op(f, *, e) === f))) by InstantiateForall(f)
      val neutrality = thenHave(f ∈ G |- ((op(e, *, f) === f) /\ (op(f, *, e) === f))) by Restate

      have((op(e, *, f) === f) /\ (op(f, *, e) === f)) by Cut(membership of (e -> f), neutrality)
      val firstEq = thenHave(op(e, *, f) === f) by Tautology

      // 2. ef = e
      have(e ∈ G |- ((op(f, *, e) === e) /\ (op(e, *, f) === e))) by InstFunSchema(Map(e -> f, f -> e))(neutrality)

      have((op(f, *, e) === e) /\ (op(e, *, f) === e)) by Cut(membership of (e -> e), lastStep)
      val secondEq = thenHave(e === op(e, *, f)) by Tautology

      // 3. Conclude by transitivity
      val eqs = have((e === op(e, *, f)) /\ (op(e, *, f) === f)) by RightAnd(secondEq, firstEq)
      have(((e === op(e, *, f)) /\ (op(e, *, f) === f)) |- (e === f)) by Tautology.from(equalityTransitivity of (x -> e, y -> op(e, *, f), z -> f))

      have(e === f) by Cut(eqs, lastStep)
    }

    have(group(G, *) |- ∃!(e, isNeutral(e, G, *))) by ExistenceAndUniqueness(isNeutral(e, G, *))(existence, uniqueness)
  }

  /**
   * Defines the identity element of `(G, *)`.
   */
  val identity = DEF(G, *) --> TheConditional(e, isNeutral(e, G, *))(identityUniqueness)

  /**
   * Theorem --- The inverse of an element `x` (i.e. `y` such that `x * y = y * x = e`) in `G` is unique.
   */
  val inverseUniqueness = Theorem(
    (group(G, *), x ∈ G) |- ∃!(y, isInverse(y, x, G, *))
  ) {
    have(group(G, *) |- ∀(x, x ∈ G ==> ∃(y, isInverse(y, x, G, *)))) by Tautology.from(group.definition, inverseExistence.definition)
    thenHave(group(G, *) |- (x ∈ G ==> ∃(y, isInverse(y, x, G, *)))) by InstantiateForall(x)
    val existence = thenHave((group(G, *), x ∈ G) |- ∃(y, isInverse(y, x, G, *))) by Restate

    // Assume y and z are inverses of x.
    // We prove the following chain of equalities:
    //   z === (yx)z === y(xz) === y
    // where equalities come from
    //   1. Left neutrality of yx
    //   2. Associativity
    //   3. Right neutrality of xz
    val uniqueness = have((group(G, *), x ∈ G, isInverse(y, x, G, *), isInverse(z, x, G, *)) |- (y === z)) subproof {
      val inverseMembership = have(isInverse(y, x, G, *) |- y ∈ G) by Tautology.from(isInverse.definition)

      // 1. (yx)z = z
      val leftNeutrality = have((group(G, *), x ∈ G, isInverse(y, x, G, *), z ∈ G) |- (op(op(y, *, x), *, z) === z)) subproof {
        assume(group(G, *))
        assume(x ∈ G)
        assume(isInverse(y, x, G, *))
        assume(z ∈ G)

        have(∀(u, u ∈ G ==> ((op(op(y, *, x), *, u) === u) /\ (op(u, *, op(y, *, x)) === u)))) by Tautology.from(isInverse.definition, isNeutral.definition of (e -> op(y, *, x)))
        thenHave(z ∈ G ==> ((op(op(y, *, x), *, z) === z) /\ (op(z, *, op(y, *, x)) === z))) by InstantiateForall(z)
        thenHave(op(op(y, *, x), *, z) === z) by Tautology
      }
      val firstEq = have((group(G, *), x ∈ G, isInverse(y, x, G, *), isInverse(z, x, G, *)) |- op(op(y, *, x), *, z) === z) by Cut(inverseMembership of (y -> z), leftNeutrality)

      // 2. (yx)z = y(xz)
      val permuteParentheses = have((group(G, *), x ∈ G, y ∈ G, z ∈ G) |- (op(op(y, *, x), *, z) === op(y, *, op(x, *, z)))) subproof {
        assume(group(G, *))
        assume(x ∈ G)
        assume(y ∈ G)
        assume(z ∈ G)

        have(∀(u, u ∈ G ==> ∀(v, v ∈ G ==> ∀(w, w ∈ G ==> (op(op(u, *, v), *, w) === op(u, *, op(v, *, w))))))) by Tautology.from(group.definition, associativity.definition)
        thenHave(y ∈ G ==> ∀(v, v ∈ G ==> ∀(w, w ∈ G ==> (op(op(y, *, v), *, w) === op(y, *, op(v, *, w)))))) by InstantiateForall(y)
        thenHave(∀(v, v ∈ G ==> ∀(w, w ∈ G ==> (op(op(y, *, v), *, w) === op(y, *, op(v, *, w)))))) by Tautology
        thenHave(x ∈ G ==> ∀(w, w ∈ G ==> (op(op(y, *, x), *, w) === op(y, *, op(x, *, w))))) by InstantiateForall(x)
        thenHave(∀(w, w ∈ G ==> (op(op(y, *, x), *, w) === op(y, *, op(x, *, w))))) by Tautology
        thenHave(z ∈ G ==> (op(op(y, *, x), *, z) === op(y, *, op(x, *, z)))) by InstantiateForall(z)
        thenHave(op(op(y, *, x), *, z) === op(y, *, op(x, *, z))) by Tautology
      }
      val associativityCut = have((group(G, *), x ∈ G /\ y ∈ G /\ z ∈ G) |- (op(op(y, *, x), *, z) === op(y, *, op(x, *, z)))) by Restate.from(permuteParentheses)
      val memberships = have((x ∈ G, isInverse(y, x, G, *), isInverse(z, x, G, *)) |- x ∈ G /\ y ∈ G /\ z ∈ G) by Tautology.from(inverseMembership of (y -> y), inverseMembership of (y -> z))
      val secondEq = have((group(G, *), x ∈ G, isInverse(y, x, G, *), isInverse(z, x, G, *)) |- op(op(y, *, x), *, z) === op(y, *, op(x, *, z))) by Cut(memberships, associativityCut)

      // 3. y(xz) = y
      val rightNeutrality = have((group(G, *), x ∈ G, y ∈ G, isInverse(z, x, G, *)) |- (op(y, *, op(x, *, z)) === y)) subproof {
        assume(group(G, *))
        assume(x ∈ G)
        assume(y ∈ G)
        assume(isInverse(z, x, G, *))

        have(∀(u, u ∈ G ==> ((op(op(x, *, z), *, u) === u) /\ (op(u, *, op(x, *, z)) === u)))) by Tautology.from(isInverse.definition of (y -> z), isNeutral.definition of (e -> op(x, *, z)))
        thenHave(y ∈ G ==> ((op(op(x, *, z), *, y) === y) /\ (op(y, *, op(x, *, z)) === y))) by InstantiateForall(y)
        thenHave(op(y, *, op(x, *, z)) === y) by Tautology
      }
      val thirdEq = have((group(G, *), x ∈ G, isInverse(y, x, G, *), isInverse(z, x, G, *)) |- op(y, *, op(x, *, z)) === y) by Cut(inverseMembership of (y -> y), rightNeutrality)

      // Conclude by transitivity

      // 4. z = y(xz)
      val fourthEq = have((group(G, *), x ∈ G, isInverse(y, x, G, *), isInverse(z, x, G, *)) |- z === op(y, *, op(x, *, z))) by Tautology.from(
        firstEq, secondEq, equalityTransitivity of (x -> z, y -> op(op(y, *, x), *, z), z -> op(y, *, op(x, *, z)))
      )

      // 5. z = y
      have((group(G, *), x ∈ G, isInverse(y, x, G, *), isInverse(z, x, G, *)) |- z === y) by Tautology.from(
        thirdEq, fourthEq, equalityTransitivity of (x -> z, y -> op(y, *, op(x, *, z)), z -> y)
      )
    }
    
    have(thesis) by ExistenceAndUniqueness(isInverse(y, x, G, *))(existence, uniqueness)
  }

  /**
   * Defines the inverse of an element `x` in a group `(G, *)`.
   */
  val inverse = DEF(x, G, *) --> TheConditional(y, isInverse(y, x, G, *))(inverseUniqueness)

  /**
   * Theorem --- `y` is the inverse of `x` iff `x` is the inverse of `y`
   */
  val inverseSymmetry = Theorem(
	group(G, *) |- ∀(x, x ∈ G ==> (isInverse(y, x, G, *) ==> isInverse(x, y, G, *)))
  ) {
    have((group(G, *), x ∈ G, isInverse(y, x, G, *)) |- isInverse(x, y, G, *)) subproof {
      assume(group(G, *))
      assume(x ∈ G)
      assume(isInverse(y, x, G, *))

      have(y ∈ G /\ isNeutral(op(x, *, y), G, *) /\ isNeutral(op(y, *, x), G, *)) by Tautology.from(isInverse.definition)
      thenHave(isNeutral(op(y, *, x), G, *) /\ isNeutral(op(x, *, y), G, *)) by Tautology
      thenHave(x ∈ G /\ isNeutral(op(y, *, x), G, *) /\ isNeutral(op(x, *, y), G, *)) by Tautology

      have(thesis) by Tautology.from(lastStep, isInverse.definition of (y -> x, x -> y))
    }
    thenHave(group(G, *) |- x ∈ G ==> (isInverse(y, x, G, *) ==> isInverse(x, y, G, *))) by Restate
    thenHave(thesis) by RightForall
  }

  /**
   * Involution of the inverse -- For all `x`, `inverse(inverse(x)) = x`.
   */
  val inverseIsInvolutive = Theorem(
    group(G, *) |- ∀(x, x ∈ G ==> (inverse(inverse(x, G, *), G, *) === x))
  ) {
    assume(group(G, *))

    val inverseCharacterization = have((group(G, *), x ∈ G) |- ((y === inverse(x, G, *)) <=> isInverse(y, x, G, *))) subproof {
      assume(group(G, *))

      have(x ∈ G |- ∀(y, (y === inverse(x, G, *)) <=> isInverse(y, x, G, *))) by Tautology.from(inverseUniqueness, inverse.definition)
      thenHave(thesis) by InstantiateForall(y)
    }

    // Prove that inverse(x) is an inverse of x
    val inverseIsInverse = have((group(G, *), x ∈ G) |- isInverse(inverse(x, G, *), x, G, *)) subproof {
      assume(group(G, *))

      have(x ∈ G |- ((inverse(x, G, *) === inverse(x, G, *)) <=> isInverse(inverse(x, G, *), x, G, *))) by Restate.from(
        inverseCharacterization of (y -> inverse(x, G, *))
      )
      thenHave(thesis) by Restate
    }

    val membership = have((group(G, *), x ∈ G) |- in(inverse(x, G, *), G)) by Tautology.from(
      inverseIsInverse, isInverse.definition of (y -> inverse(x, G, *))
    )

    // Use the symmetry of the inverse relation to prove that x is an inverse of inverse(x)
    val satisfaction = have((group(G, *), x ∈ G) |- isInverse(x, inverse(x, G, *), G, *)) subproof {
      assume(group(G, *))

      have(∀(z, z ∈ G ==> (isInverse(inverse(x, G, *), z, G, *) ==> isInverse(z, inverse(x, G, *), G, *)))) by Tautology.from(inverseSymmetry of (y -> inverse(x, G, *)))
      thenHave(x ∈ G ==> (isInverse(inverse(x, G, *), x, G, *) ==> isInverse(x, inverse(x, G, *), G, *))) by InstantiateForall(x)
      val symmetryCut = thenHave((x ∈ G, isInverse(inverse(x, G, *), x, G, *)) |- isInverse(x, inverse(x, G, *), G, *)) by Restate

      have(thesis) by Cut(inverseIsInverse, symmetryCut)
    }

    // Conclude by uniqueness
    val u = inverse(inverse(x, G, *), G, *)

    val characterization = have(in(inverse(x, G, *), G) |- ((x === u) <=> isInverse(x, inverse(x, G, *), G, *))) by Restate.from(inverseCharacterization of (x -> inverse(x, G, *), y -> x))
    val eq = have((x ∈ G, in(inverse(x, G, *), G)) |- (x === u)) by Tautology.from(satisfaction, characterization)

    have(x ∈ G |- (x === u)) by Cut(membership, eq)
    thenHave(x ∈ G ==> (x === u)) by Restate
    thenHave(thesis) by RightForall
  }

  //
  // 1.1 Subgroups
  //

  /**
   * Subgroup --- `H` is a subgroup of `(G, *)` if `H` is a subset of `G`, such that the restriction of `*` to `H` is
   * a group law for `H`, i.e. `(H, *_H)` is a group.
   *
   * We denote `H <= G` for `H` a subgroup of `G`.
   */
  val subgroup = DEF(H, G, *) --> group(G, *) /\ subset(H, G) /\ group(H, restrictedFunction(*, cartesianProduct(H, H)))

  /**
   * Lemma --- If `x` and `y` are two elements of the subgroup `H` of `(G, *)`, the pair belongs to the relation domain
   * of the parent group's operation `*`.
   *
   * Analogous to [[groupPairInOperationDomain]], except that the considered relation is different.
   */
  val subgroupPairInParentOperationDomain = Lemma(
    (subgroup(H, G, *), x ∈ H, y ∈ H) |- pair(x, y) ∈ relationDomain(*)
  ) {
    assume(subgroup(H, G, *))
    assume(x ∈ H)
    assume(y ∈ H)

    val a, b = variable

    have(subset(H, G)) by Tautology.from(subgroup.definition)
    have(∀(x, x ∈ H ==> x ∈ G)) by Tautology.from(lastStep, subset.definition of(x -> H, y -> G))
    val subsetDef = thenHave(x ∈ H ==> x ∈ G) by InstantiateForall(x)

    val left = have(x ∈ G) by Tautology.from(subsetDef)
    val right = have(y ∈ G) by Tautology.from(subsetDef of (x -> y))

    have(group(G, *)) by Tautology.from(subgroup.definition)

    have(thesis) by Tautology.from(lastStep, left, right, groupPairInOperationDomain)
  }

  /**
   * Theorem --- The subgroup operation is exactly the same as in the above group, i.e. if `(G, *)` is a group, `H` a
   * subgroup of `G`, then for elements `x, y ∈ H` we have `x ★ y = x * y`, where `★ = *_H`.
   */
  val subgroupOperation = Theorem(
    (subgroup(H, G, *), x ∈ H, y ∈ H) |- (op(x, restrictedFunction(*, cartesianProduct(H, H)), y) === op(x, *, y))
  ) {
    val H2 = cartesianProduct(H, H)
    val ★ = restrictedFunction(*, H2)
    val r = app(★, pair(x, y))

    // We characterize op(x, *, y), and show that op(x, ★, y) satisfies all requirements
    have(
      ∀(z, (z === app(*, pair(x, y))) <=> (((functional(*) /\ in(pair(x, y), relationDomain(*))) ==> in(pair(pair(x, y), z), *)) /\
                                          ((!functional(*) \/ !in(pair(x, y), relationDomain(*))) ==> (z === ∅))))
    ) by Tautology.from(app.definition of (f -> *, x -> pair(x, y)), functionApplicationUniqueness)
    val characterization = thenHave(
      (r === app(*, pair(x, y))) <=> (((functional(*) /\ in(pair(x, y), relationDomain(*))) ==> in(pair(pair(x, y), r), *)) /\
                                     ((!functional(*) \/ !in(pair(x, y), relationDomain(*))) ==> (r === ∅)))
    ) by InstantiateForall(r)

    // Prove that the premises of the first implication hold
    val leftPremise = have(subgroup(H, G, *) |- functional(*)) subproof {
      assume(subgroup(H, G, *))
      have(group(G, *)) by Tautology.from(subgroup.definition)
      have(functional(*)) by Tautology.from(lastStep, groupOperationIsFunctional)
    }

    val premises = have((subgroup(H, G, *), x ∈ H, y ∈ H) |- functional(*) /\ pair(x, y) ∈ relationDomain(*)) by RightAnd(
      leftPremise, subgroupPairInParentOperationDomain
    )

    // We show that op(x, ★, y) satisfies the conclusion of the implication
    val appDef = have(
      (functional(★), pair(x, y) ∈ relationDomain(★)) |- pair(pair(x, y), r) ∈ ★
    ) by Definition(app, functionApplicationUniqueness)(★, pair(x, y))

    // Reduce the assumptions of the definition to our subgroup assumption
    val reduction1 = have(subgroup(H, G, *) |- group(H, ★)) by Tautology.from(subgroup.definition)
    val reduction2 = have(subgroup(H, G, *) |- functional(★)) by Tautology.from(lastStep, groupOperationIsFunctional of (G -> H, * -> ★))

    val reduction3 = have((subgroup(H, G, *), pair(x, y) ∈ relationDomain(★)) |- pair(pair(x, y), r) ∈ ★) by Tautology.from(reduction2, appDef)

    have((subgroup(H, G, *), x ∈ H, y ∈ H) |- pair(x, y) ∈ relationDomain(★)) by Cut(
      reduction1, groupPairInOperationDomain of (G -> H, * -> ★)
    )
    val reducedDef = have((subgroup(H, G, *), x ∈ H, y ∈ H) |- pair(pair(x, y), r) ∈ ★) by Cut(lastStep, reduction3)

    have(∀(u, (u ∈ ★) <=> (u ∈ * /\ ∃(y, ∃(z, y ∈ H2 /\ (u === pair(y, z))))))) by Definition(restrictedFunction, restrictedFunctionUniqueness)(*, H2)
    thenHave((u ∈ ★) <=> (u ∈ * /\ ∃(y, ∃(z, y ∈ H2 /\ (u === pair(y, z)))))) by InstantiateForall(u)
    thenHave(u ∈ ★ ==> u ∈ *) by Tautology

    val satisfaction = have((subgroup(H, G, *), x ∈ H, y ∈ H) |- pair(pair(x, y), r) ∈ *) by Tautology.from(
      lastStep of (u -> pair(pair(x, y), r)), reducedDef
    )

    // Reconstruct the whole definition
    assume(subgroup(H, G, *))
    assume(x ∈ H)
    assume(y ∈ H)

    val pos = have((functional(*) /\ pair(x, y) ∈ relationDomain(*)) ==> pair(pair(x, y), r) ∈ *) by Tautology.from(premises, satisfaction)

    have(!(functional(*) /\ pair(x, y) ∈ relationDomain(*)) |- ()) by LeftNot(premises)
    thenHave(!functional(*) \/ !(pair(x, y) ∈ relationDomain(*)) |- ()) by Restate
    thenHave(!functional(*) \/ !(pair(x, y) ∈ relationDomain(*)) |- (r === emptySet())) by Weakening
    val neg = thenHave((!functional(*) \/ !(pair(x, y) ∈ relationDomain(*))) ==> (r === emptySet())) by Restate

    have(
      ((functional(*) /\ pair(x, y) ∈ relationDomain(*)) ==> pair(pair(x, y), r) ∈ *) /\
      ((!functional(*) \/ !(pair(x, y) ∈ relationDomain(*))) ==> (r === emptySet()))
    ) by RightAnd(pos, neg)

    have(thesis) by Tautology.from(lastStep, characterization)
  }
}