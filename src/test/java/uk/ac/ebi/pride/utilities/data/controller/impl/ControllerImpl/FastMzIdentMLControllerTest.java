package uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.pride.utilities.data.controller.DataAccessController;
import uk.ac.ebi.pride.utilities.data.core.*;

import java.io.File;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Most of the public methods of FastMzIdentMLController required for the validation are tested by FastMzIdentMLControllerTest class.
 *
 * @author Suresh Hewapathirana
 */
@Slf4j
public class FastMzIdentMLControllerTest {

  private FastMzIdentMLController fastMzIdentMLController;

  /**
   * This is the initial method which instantiates FastMzIdentMLController and spectra file(s) to the controller.
   * This will make sure to populate any cache data that are required for the fast validation.
   *
   * @throws Exception Any Exception
   */
  @Before
  public void setUp() throws Exception {
    URL url = FastMzIdentMLControllerTest.class.getClassLoader().getResource("small.mzid");
    URL urlMgf = MzIdentMLControllerIterativeTest.class.getClassLoader().getResource("small.mgf");

    if (url == null || urlMgf == null) {
      throw new IllegalStateException("no file for input found!");
    }
    File inputFile = new File(url.toURI());
    fastMzIdentMLController = new FastMzIdentMLController(inputFile);

    // add spectra file(s) to the FastMzIdentMLController
    List<File> files = new ArrayList<>();
    files.add(new File(urlMgf.toURI()));
    fastMzIdentMLController.addMSController(files);
    // run initial inspection
    fastMzIdentMLController.doSpectraValidation();
  }

  /**
   * A method to print validation results. This provides basic statistics about the MzIdentML file.
   */
  @Test
  public void printValidationsResults() {
    log.info("Protein Counts          : " + fastMzIdentMLController.getNumberOfProteins());
    log.info("Peptide Counts          : " + fastMzIdentMLController.getNumberOfPeptides());
    log.info("PeptidoForms Counts     : " + fastMzIdentMLController.getNumberOfPeptidoForms());
    log.info("Unique Peptide Counts   : " + fastMzIdentMLController.getNumberOfUniquePeptides());
    log.info("Spectrum Count          : " + fastMzIdentMLController.getNumberOfSpectra());
    log.info("Missing Spectrum Count  : " + fastMzIdentMLController.getNumberOfMissingSpectra());
    log.info("Missing Spectra ID List : " + fastMzIdentMLController.getMissingIdentifiedSpectraIds().toString());
    log.info("Identified Spectrum Count: " + fastMzIdentMLController.getNumberOfIdentifiedSpectra());
    log.info("DeltaMz Error Rate      : " + fastMzIdentMLController.getSampleDeltaMzErrorRate(100, 4.0));
    log.info("Identified Unique PTMs  : " + fastMzIdentMLController.getIdentifiedUniquePTMs().toString());
    log.info("Search Modifications    : " + fastMzIdentMLController.getSearchModifications().toString());
  }

  /**
   * A common method to run all the mata data test methods
   */
  @Test
  public void scanMetadata(){
    scanForGeneralMetadata(fastMzIdentMLController);
    scanForSoftware(fastMzIdentMLController);
    scanForSearchDetails(fastMzIdentMLController);
  }

  /**
   * Test Number of Identified proteins
   */
  @Test
  public void getNumberOfProteins() {
    assertEquals("Total number of proteins in the MzIdentML file should be 327", 327, fastMzIdentMLController.getNumberOfProteins());
  }

  /**
   *Test Number of Peptides identified: Total number of peptide Sequences identified.
   */
  @Test
  public void getNumberOfPeptides() {
    assertEquals("Total number of Peptide in the MzIdentML file should be 1956", 1956, fastMzIdentMLController.getNumberOfPeptides());
  }

  /**
   * Test Number of PeptidoForms: Number of Peptides + modifications + position of the modification.
   */
  @Test
  public void getNumberOfPeptidoForms() {
    assertEquals("Total number of PeptidoForms in the MzIdentML file should be 3757", 3757, fastMzIdentMLController.getNumberOfPeptidoForms());
    assertTrue("Total number of PeptidoForms should be greater than or equal to number of peptides", fastMzIdentMLController.getNumberOfPeptides() >= fastMzIdentMLController.getNumberOfPeptides());
  }

  /**
   * Test Number of Unique Peptides: Peptides that map uniquely to a Protein ID.
   */
  @Test
  public void getNumberOfUniquePeptides() {
    assertEquals("Total number of Peptide in the MzIdentML file should be 1956", 1956, fastMzIdentMLController.getNumberOfUniquePeptides());
  }

  /**
   * Test Number of identified Spectra: Number of MS/MS with at least one peptide identification.
   */
  @Test
  public void getNumberOfSpectra() {
    assertEquals("Total number of Spectra in the MzIdentML file should be 1001", 1001, fastMzIdentMLController.getNumberOfSpectra());
  }

  /**
   * Test Number of PSMs reported in the MzIdentML, but not available in the peak file(s)
   */
  @Test
  public void getNumberOfMissingSpectra() {
    assertEquals("Total number of missing spectra in the MzIdentML file should be 0", 0, fastMzIdentMLController.getNumberOfMissingSpectra());
  }

  /**
   * Test Number of PSMs reported in the MzIdentML
   */
  @Test
  public void getNumberOfIdentifiedSpectra() {
    assertEquals("Total number of identified spectra in the MzIdentML file should be 851", 851, fastMzIdentMLController.getNumberOfIdentifiedSpectra());
  }

  /**
   * Randomly check if the delta mass is within the threshold specified
   */
  @Test
  public void checkRandomSpectraByDeltaMassThreshold() {
    final double DELTA_MZ = 4.0;
    assertTrue("DeltaMz Error Rate should be less than " + DELTA_MZ, fastMzIdentMLController.getSampleDeltaMzErrorRate(10, DELTA_MZ) < DELTA_MZ);
  }

  /**
   * Test Missing Spectra
   */
  @Test
  public void getMissingIdentifiedSpectraIds() {
    assertEquals("Total number of missing spectra in the MzIdentML file should be 0", 0, fastMzIdentMLController.getMissingIdentifiedSpectraIds().size());
  }

  /**
   * Test Uniquely identified Post translational modifications
   */
  @Test
  public void getIdentifiedUniquePTMs() {
    assertEquals("Total number of identified unique PTMs should be 3", 3, fastMzIdentMLController.getIdentifiedUniquePTMs().size());
    log.info(fastMzIdentMLController.getIdentifiedUniquePTMs().toString());
  }

  /**
   * Test search modifications
   */
  @Test
  public void getSearchModifications() {
    assertEquals("Total number of search modifications should be 2", 2, fastMzIdentMLController.getSearchModifications().size());
    log.info(fastMzIdentMLController.getSearchModifications().toString());
  }

  /**
   * Test anchor proteins: Here we need to report only the proteins that are the anchor of the protein group in MzIdentML.
   */
  @Test
  public void getAnchorProteinIds() {
    assertEquals("Total number of protein ambiguity groups  should be 1", 1, fastMzIdentMLController.getAnchorProteinIds().size());
  }

  /**
   * Test experiment metadata
   */
  @Test
  public void getExperimentMetaData() {
    fastMzIdentMLController.getExperimentMetaData();
  }

  /**
   * Test retrieving enzyme
   */
  @Test
  public void getEnzymes() {
    System.out.println(fastMzIdentMLController.getEnzymes());
  }

  private void scanForGeneralMetadata(DataAccessController dataAccessController) {
    ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();

    System.out.println("MzIdentML Version : " + experimentMetaData.getVersion());
    System.out.println("Experiment Id     : " + experimentMetaData.getId());
    System.out.println("Experiment Name   : " + experimentMetaData.getName());
    System.out.println("Experiment Title  : " + experimentMetaData.getShortLabel());
    System.out.println("Protocol          : " + experimentMetaData.getProtocol());
    System.out.println("Sources           : " + experimentMetaData.getSourceFiles().toString());
    System.out.println("Software          : " + experimentMetaData.getSoftwares().toString());
    System.out.println("Contact Person    : " + experimentMetaData.getPersons().toString());
    System.out.println("Organizations     : " + experimentMetaData.getOrganizations().toString());
    System.out.println("Provider          : " + experimentMetaData.getProvider().toString());
    System.out.println("CreationDate      : " + experimentMetaData.getCreationDate().toString());
    System.out.println("References        : " + experimentMetaData.getReferences().toString());
    System.out.println("SpectraData       : " + experimentMetaData.getSpectraDatas().toString());
    System.out.println("Samples           : " + experimentMetaData.getSamples());
    System.out.println("Additional        : " + experimentMetaData.getAdditional().toString());

    System.out.println("Enzymes      : " + fastMzIdentMLController.getFormattedEnzymes());
  }

  private void scanForSoftware(DataAccessController dataAccessController) {
    ExperimentMetaData experimentMetaData = dataAccessController.getExperimentMetaData();
    Set<Software> softwares = new HashSet<>(experimentMetaData.getSoftwares());
    log.info("Softwares: " + softwares);
  }

  private void scanForSearchDetails(DataAccessController dataAccessController) {
    Collection<Comparable> proteinIds = dataAccessController.getProteinIds();
    if (proteinIds != null && !proteinIds.isEmpty()) {
      Comparable firstProteinId = proteinIds.iterator().next();
      String accession = dataAccessController.getProteinAccession(firstProteinId);
      log.info("First Protein: " + accession);
      SearchDataBase searchDatabase = dataAccessController.getSearchDatabase(firstProteinId);
      if (searchDatabase != null) {
        log.info("Search Database: " + searchDatabase.getName());
      }
    }
  }

  /**
   * clear the MzIdentML object which holds all the data in memory
   */
  @After
  public void tearDown() {
    fastMzIdentMLController.close();
  }
}