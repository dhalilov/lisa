package lisa.settheory

import lisa.kernel.fol.FOL.*
import lisa.utils.KernelHelpers.{_, given}

/**
 * Axioms for the Tarski-Grothendieck theory (TG)
 */
private[settheory] trait SetTheoryTGAxioms extends SetTheoryZFAxioms {
  private val (x, y, z) =
    (VariableLabel("x"), VariableLabel("y"), VariableLabel("z"))

  final val tarskiAxiom: runningSetTheory.Axiom = runningSetTheory.makeAxiom(
    forall(
      x,
      in(x, universe(x)) /\
        forall(
          y,
          in(y, universe(x)) ==> (in(powerSet(y), universe(x)) /\ subset(powerSet(y), universe(x))) /\
            forall(z, subset(z, universe(x)) ==> (sim(y, universe(x)) /\ in(y, universe(x))))
        )
    )
  )

  override def axioms: Set[(String, runningSetTheory.Axiom)] = super.axioms + (("TarskiAxiom", tarskiAxiom))

}
