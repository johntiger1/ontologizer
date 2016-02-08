package ontologizer.calculation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;

/**
 * Simple class for testing representing a typical simulation
 * setting.
 *
 * @author Sebastian Bauer
 */
public class SingleCalculationSetting
{
	public PopulationSet pop;
	public StudySet study;

	/**
	 * Sample from the entire population defined by the association container a study set with given false-positive
	 * and false-negative rates.
	 *
	 * @param rnd the random generator to use
	 * @param wantedActiveTerms the terms that should be over represented. The value for each entry represents the false negative-rate of the term, i.e.,
	 *  how many items are removed in average.
	 * @param alphaStudySet false positive rate, i.e., how many item are included randomily.
	 * @param ontology the underlying ontology
	 * @param assoc the container holding associations between items and terms.
	 * @return
	 */
	public static SingleCalculationSetting create(Random rnd, HashMap<TermID, Double> wantedActiveTerms, double alphaStudySet, Ontology ontology, AssociationContainer assoc)
	{
		SingleCalculationSetting scs = new SingleCalculationSetting();

		PopulationSet allGenes = new PopulationSet("all");
		scs.pop = allGenes;
		for (ByteString gene : assoc.getAllAnnotatedGenes())
			allGenes.addGene(gene, "");

		GOTermEnumerator allEnumerator = allGenes.enumerateGOTerms(ontology,assoc);

		/* Create for each wanted term an study set for its own */
		HashMap<TermID,StudySet> wantedActiveTerm2StudySet = new HashMap<TermID,StudySet>();
		for (TermID t : wantedActiveTerms.keySet())
		{
			StudySet termStudySet = new StudySet("study");
			for (ByteString g : allEnumerator.getAnnotatedGenes(t).totalAnnotated)
				termStudySet.addGene(g, "");
			termStudySet.filterOutDuplicateGenes(assoc);
			wantedActiveTerm2StudySet.put(t, termStudySet);
		}

		/* Combine the study sets into one */
		StudySet newStudyGenes = new StudySet("study");
		scs.study = newStudyGenes;

		for (TermID t : wantedActiveTerms.keySet())
			newStudyGenes.addGenes(wantedActiveTerm2StudySet.get(t));
		newStudyGenes.filterOutDuplicateGenes(assoc);

		int tp = newStudyGenes.getGeneCount();
		int tn = allGenes.getGeneCount();

		/* Obfuscate the study set, i.e., create the observed state */

		/* false -> true (alpha, false positive) */
		HashSet<ByteString>  fp = new HashSet<ByteString>();
		for (ByteString gene : allGenes)
		{
			if (newStudyGenes.contains(gene)) continue;
			if (rnd.nextDouble() < alphaStudySet) fp.add(gene);
		}

		/* true -> false (beta, false negative) */
		HashSet<ByteString>  fn = new HashSet<ByteString>();
		for (TermID t : wantedActiveTerms.keySet())
		{
			double beta = wantedActiveTerms.get(t);
			StudySet termStudySet = wantedActiveTerm2StudySet.get(t);
			for (ByteString g : termStudySet)
			{
				if (rnd.nextDouble() < beta) fn.add(g);
			}
		}

		newStudyGenes.addGenes(fp);
		newStudyGenes.removeGenes(fn);
		return scs;
	}
}