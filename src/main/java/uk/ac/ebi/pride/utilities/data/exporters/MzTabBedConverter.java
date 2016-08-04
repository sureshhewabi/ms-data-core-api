package uk.ac.ebi.pride.utilities.data.exporters;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import uk.ac.ebi.pride.utilities.data.controller.impl.ControllerImpl.MzTabControllerImpl;
import uk.ac.ebi.pride.utilities.data.core.*;
import uk.ac.ebi.pride.utilities.data.core.Modification;
import uk.ac.ebi.pride.utilities.data.core.Peptide;
import uk.ac.ebi.pride.utilities.data.core.Protein;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


/**
 * Class to convert MzTab files to Bed files. Requires chromosome information to be present.
 *
 * @author Tobias Ternent tobias@ebi.ac.uk
 */
public class MzTabBedConverter {

    protected static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MzTabBedConverter.class);

    private MzTabControllerImpl mzTabController;
    private String projectAccession;
    private String assayAccession;
    private boolean proteoAnnotatorReporcessed;
    private final static String PROBED_VERSION = "1.0";

    /**
     * Constructor to setup conversion of an mzTabFile into a bed file.
     * @param mzTabFile to be converted to a bed file.
     */
    public MzTabBedConverter(MzTabControllerImpl mzTabFile) {
        this(mzTabFile, "", "", false);
    }

    public MzTabBedConverter(MzTabControllerImpl mzTabFile, boolean proteoAnnotatorReporcessed) {
        this(mzTabFile, "", "", proteoAnnotatorReporcessed);
    }

    public MzTabBedConverter(MzTabControllerImpl mzTabFile, String projectAccession) {
        this(mzTabFile, projectAccession, "", false);
    }

    public MzTabBedConverter(MzTabControllerImpl mzTabFile, String projectAccession, boolean proteoAnnotatorReporcessed) {
        this(mzTabFile, projectAccession, "", proteoAnnotatorReporcessed);
    }

    public MzTabBedConverter(MzTabControllerImpl mzTabFile, String projectAccession, String assayAccession) {
        this(mzTabFile, projectAccession, assayAccession, false);
    }

    public MzTabBedConverter(MzTabControllerImpl mzTabFile, String projectAccession, String assayAccession, boolean proteoAnnotatorReporcessed) {
        this.mzTabController = mzTabFile;
        this.projectAccession = projectAccession;
        this.assayAccession = assayAccession;
        this.proteoAnnotatorReporcessed = proteoAnnotatorReporcessed;
    }

    /**
     * Performs the conversion of the mzTabFile into a bed file.
     * @param outputFile is the generated output bed file,
     * @throws Exception
     */
    public void convert(File outputFile) throws Exception {
        FileWriter file = new FileWriter(outputFile.getPath());
        BufferedWriter bf = new BufferedWriter(file);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.setLength(0);
        int lineNumber = 1;

        List<String> allPeptideSequences = new ArrayList<>();
        for (Comparable proteinID : mzTabController.getProteinIds()) {
            ArrayList<PeptideEvidence> evidences = new ArrayList<>();
            for (Peptide peptide : mzTabController.getProteinById(proteinID).getPeptides()) {
                for (PeptideEvidence peptideEvidence : peptide.getPeptideEvidenceList()) {
                    if (!evidences.contains(peptideEvidence)) {
                        evidences.add(peptideEvidence);
                        if (hasChromCvps(peptideEvidence.getCvParams())) {
                            allPeptideSequences.add(peptideEvidence.getPeptideSequence().getSequence());
                        }
                    }
                }
            }
        }
        Set<String> duplicatePeptideSequences = new HashSet<>();
        Set<String> tempePeptideSequences = new HashSet<>();
        for (String peptideSequence : allPeptideSequences) {
            if (!tempePeptideSequences.add(peptideSequence)) {
                duplicatePeptideSequences.add(peptideSequence);
            }
        }

        HashMap<Comparable, ProteinGroup> proteinGroupHashMap = new HashMap<>();
        if (mzTabController.hasProteinAmbiguityGroup()) {
            Collection<Comparable>  groupIDs = mzTabController.getProteinAmbiguityGroupIds();
            proteinGroupHashMap = new HashMap<>();
            for (Comparable groupID : groupIDs) {
                proteinGroupHashMap.put(groupID, mzTabController.getProteinAmbiguityGroupById(groupID));
            }
        }
        for (Comparable proteinID : mzTabController.getProteinIds()) {
            Protein protein = mzTabController.getProteinById(proteinID);
            ArrayList<PeptideEvidence> evidences = new ArrayList<>();
            for (Peptide peptide : protein.getPeptides()) {
                for (PeptideEvidence peptideEvidence : peptide.getPeptideEvidenceList()) {
                    if (!evidences.contains(peptideEvidence)) {
                        evidences.add(peptideEvidence);
                        String chrom = ".", chromstart = ".", chromend = ".", strand = ".", pepMods = ".",
                                psmScore = ".", fdrScore = ".",
                                blockStarts = ".", blockSizes = ".", blockCount="1", buildVersion=".";
                        for (CvParam cvParam : peptideEvidence.getCvParams()) {
                            switch (cvParam.getAccession()) {
                                case ("MS:1002637"):
                                    chrom = cvParam.getValue();
                                    break;
                                case ("MS:1002643"):
                                    String[] starts = checkFixEndComma(cvParam.getValue()).split(",");
                                    chromstart = starts[0];
                                    for (int i=0; i<starts.length; i++) {
                                        starts[i] = "" + (Integer.parseInt(starts[i]) - Integer.parseInt(chromstart));
                                    }
                                    blockStarts = StringUtils.join(starts, ",");
                                    break;
                                case ("MS:1002641"):
                                    blockCount = cvParam.getValue();
                                    break;
                                case ("MS:1002638"):
                                    strand = cvParam.getValue();
                                    break;
                                case ("MS:1002642"):
                                    blockSizes = checkFixEndComma(cvParam.getValue());
                                    break;
                                case ("MS:1002356"):
                                    fdrScore = cvParam.getValue();
                                    break;
                                case ("MS:1002345"):
                                    psmScore = cvParam.getValue();
                                    break;
                                case ("MS:1002644"):
                                    buildVersion = FilenameUtils.removeExtension(cvParam.getValue());
                                    break;
                                default:
                                    break;
                                // todo
                                // has protein group/inference?
                                // has proteindetectionprotocol thresholding?
                                // grouping PSMs?
                            }
                        }
                       ArrayList<String> peptideModifications = new ArrayList<>();
                        for (Modification modification : peptideEvidence.getPeptideSequence().getModifications()) {
                            for (CvParam cvParam : modification.getCvParams()) {
                                peptideModifications.add(modification.getLocation() + "-" + cvParam.getAccession());
                            }
                        }
                        if (peptideModifications.size() > 0) {
                            pepMods = StringUtils.join(peptideModifications, ", ");
                        }
                        if (!chrom.equalsIgnoreCase(".")) {
                            String[] starts = blockStarts.split(",");
                            String[] sizes = blockSizes.split(",");
                            chromend = "" + (Integer.parseInt(chromstart) + Integer.parseInt(starts[starts.length-1]) + Integer.parseInt(sizes[sizes.length-1]));

                            stringBuilder.append(chrom); // chrom
                            stringBuilder.append('\t');
                            stringBuilder.append(chromstart); // chromstart
                            stringBuilder.append('\t');
                            stringBuilder.append(chromend); // chromend
                            stringBuilder.append('\t');
                            String name =  protein.getDbSequence().getName();
                            if (!projectAccession.isEmpty()) {
                                name = name + "_" + projectAccession;
                            }
                            if (!assayAccession.isEmpty()) {
                                name = name + "_" + assayAccession;
                            }
                            name = name + "_" + ++lineNumber;
                            stringBuilder.append(name); // name
                            stringBuilder.append('\t');
                            final int TOTAL_EVIDENCES = peptide.getPeptideEvidenceList().size();
                            if (mzTabController.hasProteinAmbiguityGroup()) {
                                final int RANGE = 110;
                                int difference = 0;
                                double zeroRange = 1.00;
                                double oneRange = 0.5;
                                double twoRange = 0.25;
                                int constant = 0; /*
                                <167	1
                                167-277	2-4
                                278-388	5-7
                                389-499	8-10
                                500-610	11-13
                                611-722	14-16
                                723-833	17-19
                                834-944	20-22
                                >944	>22     */
                                if (TOTAL_EVIDENCES==1) {
                                    constant = 166;
                                } else if (TOTAL_EVIDENCES>1 && TOTAL_EVIDENCES<5) {
                                    difference = 4-TOTAL_EVIDENCES;
                                    constant = 167;
                                } else if (TOTAL_EVIDENCES>4 && TOTAL_EVIDENCES<8) {
                                    difference = 7-TOTAL_EVIDENCES;
                                    constant = 278;
                                } else if (TOTAL_EVIDENCES>7 && TOTAL_EVIDENCES<11) {
                                    difference = 10-TOTAL_EVIDENCES;
                                    constant = 389;
                                } else if (TOTAL_EVIDENCES>10 && TOTAL_EVIDENCES<14) {
                                    difference = 13-TOTAL_EVIDENCES;
                                    constant = 500;
                                } else if (TOTAL_EVIDENCES>13 && TOTAL_EVIDENCES<17) {
                                    difference = 16-TOTAL_EVIDENCES;
                                    constant = 611;
                                } else if (TOTAL_EVIDENCES>16 && TOTAL_EVIDENCES<20) {
                                    difference = 19-TOTAL_EVIDENCES;
                                    constant = 723;
                                } else if (TOTAL_EVIDENCES>19 && TOTAL_EVIDENCES<23) {
                                    difference = 22-TOTAL_EVIDENCES;
                                    constant = 834;
                                } else if (TOTAL_EVIDENCES>22) {
                                    constant = 1000;
                                }
                                double chosenComponent;
                                switch (difference) {
                                    case 0:
                                        chosenComponent = zeroRange;
                                        break;
                                    case 1:
                                        chosenComponent = oneRange;
                                        break;
                                    case 2:
                                        chosenComponent = twoRange;
                                        break;
                                    default:
                                        chosenComponent = 0.0;
                                        break;
                                }
                                stringBuilder.append(new Double(Math.floor((chosenComponent * RANGE) + constant)).intValue());
                                // score, according to evidence
                            } else {
                                stringBuilder.append(1000);
                            } // score, no PSM group : 1000 TODO test
                            stringBuilder.append('\t');
                            stringBuilder.append(strand); // strand
                            stringBuilder.append('\t');
                            stringBuilder.append(chromstart); // thickStart
                            stringBuilder.append('\t');
                            stringBuilder.append(chromend); // thickEnd
                            stringBuilder.append('\t');
                            stringBuilder.append("0"); // reserved - (0 only)
                            stringBuilder.append('\t');
                            stringBuilder.append(blockCount); // blockCount
                            stringBuilder.append('\t');
                            stringBuilder.append(blockSizes); // blockSizes
                            stringBuilder.append('\t');
                            stringBuilder.append(blockStarts); // chromStarts (actual name, but refers to blocks)
                            stringBuilder.append('\t') ;
                            stringBuilder.append(protein.getDbSequence().getName()); // proteinAccession
                            stringBuilder.append('\t') ;
                            stringBuilder.append(peptideEvidence.getPeptideSequence().getSequence());  // peptideSequence
                            stringBuilder.append('\t');
                            if (!duplicatePeptideSequences.contains(peptide.getSequence())) {
                                stringBuilder.append("unique");
                            } else {
                                HashSet<ProteinGroup> groups = getProtgeinGroups(proteinID, proteinGroupHashMap);
                                int evidenceOnThisLoci = 0, evidenceOnOtherLoci = 0;
                                Collection<Comparable> proteinIDs = new HashSet<>();
                                if (groups.size()>0) {
                                    for (ProteinGroup group : groups) {
                                            proteinIDs.addAll(group.getProteinIds());
                                    }
                                } else {
                                    proteinIDs = mzTabController.getProteinIds();
                                }
                                for (Comparable proteinIdToCheck : proteinIDs) {
                                    List<Peptide> peptidesList = mzTabController.getProteinById(proteinIdToCheck).getPeptides();
                                    for (Peptide peptideToCheck : peptidesList) {
                                        if (!peptideToCheck.equals(peptide) && peptideToCheck.getSequence().equals(peptide.getSequence())) {
                                            List<CvParam> cvParams = peptideToCheck.getPeptideEvidence().getCvParams();
                                            Map<String, CvParam> cvps = new HashMap<>();
                                            for (CvParam cvp : cvParams) {
                                                cvps.put(cvp.getAccession(), cvp);
                                            }
                                            if (hasChromCvps(peptideToCheck.getPeptideEvidence().getCvParams())) {
                                                String cvpChrom = cvps.get("MS:1002637").getValue();
                                                String cvpStart = checkFixEndComma(cvps.get("MS:1002643").getValue()).split(",")[0];
                                                String cvpEnd = cvps.get("MS:1002640").getValue();
                                                if (cvpChrom.equalsIgnoreCase(chrom) && (
                                                        ((Integer.parseInt(cvpStart)-18)<=Integer.parseInt(chromstart) && (Integer.parseInt(cvpStart)+18>=Integer.parseInt(chromstart)))
                                                      && (Integer.parseInt(cvpEnd)-18)<=Integer.parseInt(chromend) &&  (Integer.parseInt(cvpEnd)+18>=(Integer.parseInt(chromend))))) {
                                                    evidenceOnThisLoci++;
                                                } else {
                                                    evidenceOnOtherLoci++;
                                                }
                                            }

                                        }
                                    }
                                 }
                                final int RANGE = 4;
                                if (evidenceOnOtherLoci==0 && evidenceOnThisLoci==0) {
                                    stringBuilder.append("not-unique[conflict]");
                                } else {
                                    if ((evidenceOnOtherLoci - evidenceOnThisLoci) > RANGE) {
                                        stringBuilder.append("not-unique[subset]");
                                    } else if ((evidenceOnOtherLoci - evidenceOnThisLoci) < RANGE){
                                        stringBuilder.append("not-unique[same-set]");
                                    } else {
                                        stringBuilder.append("not-unique[unknown]");
                                    }
                                }
                            } // peptide uniqueness
                            stringBuilder.append('\t');
                            stringBuilder.append(buildVersion); // buildVersion
                            stringBuilder.append('\t');
                            if (psmScore==null || psmScore.isEmpty() || psmScore.equalsIgnoreCase(".")) {
                                for (CvParam cvp :  protein.getCvParams()) {
                                    if (cvp.getAccession().equalsIgnoreCase("MS:1002235")) {
                                        psmScore = cvp.getValue();
                                        if (psmScore==null || psmScore.isEmpty()) {
                                            psmScore = ".";
                                        }
                                        break;
                                    }
                                }
                            }
                            stringBuilder.append(psmScore); // psmScore
                            stringBuilder.append('\t');
                            stringBuilder.append(fdrScore); // fdr
                            stringBuilder.append('\t');
                            stringBuilder.append(pepMods); // peptide location modifications
                            stringBuilder.append('\t');
                            stringBuilder.append(mzTabController.hasProteinAmbiguityGroup() ? "." : peptide.getPrecursorCharge()); // charge (null if row = group of PSMs)
                            stringBuilder.append('\t');
                            stringBuilder.append(mzTabController.hasProteinAmbiguityGroup() ? "." : peptide.getSpectrumIdentification().getExperimentalMassToCharge()); // expMassToCharge (null if row = group of PSMs)
                            stringBuilder.append('\t');
                            stringBuilder.append(mzTabController.hasProteinAmbiguityGroup() ? "." : peptide.getSpectrumIdentification().getCalculatedMassToCharge()); // calcMassToCharge (null if row = group of PSMs)
                            stringBuilder.append('\t');
                            stringBuilder.append(mzTabController.hasProteinAmbiguityGroup() ? "." : peptide.getSpectrumIdentification().getRank()); // PSM rank (null if row = group of PSMs)
                            stringBuilder.append('\t');
                            String datasetID;
                            if (!projectAccession.isEmpty()) {
                                datasetID = proteoAnnotatorReporcessed ? projectAccession + "_proteoannotator_reprocessed"
                                                                       : projectAccession;
                                if (!assayAccession.isEmpty()) {
                                    datasetID = datasetID + "_" + assayAccession;
                                }
                            } else {
                                datasetID = mzTabController.getReader().getMetadata().getMZTabID();
                            }
                            stringBuilder.append(datasetID); // datasetID
                            stringBuilder.append('\t');
                            String projectUri ;
                            if (!projectAccession.isEmpty()) {
                                projectUri = proteoAnnotatorReporcessed ?
                                        "http://ftp.pride.ebi.ac.uk/pride/data/proteogenomics/latest/proteoannotator/reprocessed_data/" + projectAccession :
                                        "http://www.ebi.ac.uk/pride/archive/projects/" + projectAccession;
                            }  else {
                                projectUri = ".";
                            }
                            stringBuilder.append(projectUri); // projectURI
                            stringBuilder.append('\n');
                            bf.write(stringBuilder.toString());
                            bf.flush();
                        }
                        stringBuilder.setLength(0);
                    }
                }
            }
        }
        bf.close();
    }

    private String checkFixEndComma(String input) {
        return (input.charAt(input.length()-1)==',' ? input.substring(0, input.length()-1) : input);
    }

    private HashSet<ProteinGroup> getProtgeinGroups(Comparable proteinID, HashMap<Comparable, ProteinGroup> proteinGroupHashMap) {
        HashSet<ProteinGroup> result = new HashSet<>();
        Collection<Comparable> keys = proteinGroupHashMap.keySet();
        for (Comparable groupID : keys) {
            ProteinGroup proteinGroup = proteinGroupHashMap.get(groupID);
            if (proteinGroup.getProteinIds().contains(proteinID)) {
                result.add(proteinGroup);
            }
        }
        return result;
    }

    private boolean hasChromCvps(Collection<CvParam> cvParams) {
        Map<String, CvParam> cvps = new HashMap<>();
        for (CvParam cvp : cvParams) {
            cvps.put(cvp.getAccession(), cvp);
        }
        return cvps.containsKey("MS:1002637") && cvps.containsKey("MS:1002643") && cvps.containsKey("MS:1002640");
    }

    public static File sortProBed(File inputProBed, File inputChromSizes) throws IOException, InterruptedException {
        logger.info("Input unsorted BED file: " + inputProBed.getPath());
        logger.info("Input chrom  file: " + inputChromSizes.getPath());
        File tempSortedBedFile =  new File(inputProBed.getPath() + ".sorted_tmp");
        File sortedProBed = new File(inputProBed.getParentFile().getPath() + File.separator + FilenameUtils.getBaseName(inputProBed.getName()) + "_sorted.pro.bed");
        tempSortedBedFile.createNewFile();
        sortedProBed.createNewFile();
        logger.info("Sorting BED file: " + inputProBed.getPath());
        logger.info("Writing to sorted pro bed file, filtered by chrom names: " + sortedProBed.getPath());
        List<String> lines = Files.readAllLines(inputChromSizes.toPath(), Charset.defaultCharset());
        Set chromNames = new TreeSet();
        for (String line : lines) {
            String[] chromLine = line.split("\t");
            chromNames.add(chromLine[0]);
        }
        List<String> sortedLines;

        try (Stream<String> stream = Files.lines(inputProBed.toPath())) {
            sortedLines = stream.sorted((o1, o2) -> {
                String firstKey1 = o1.substring(0, o1.indexOf('\t'));
                String firstKey2 = o2.substring(0, o2.indexOf('\t'));
                int aComp = firstKey1.compareTo(firstKey2);
                if (aComp != 0) {
                    return aComp; //1st key by 1st column (chrom name) as String
                } else {
                    String secondKey1 = o1.substring(StringUtils.ordinalIndexOf(o1, "\t", 1)+1, StringUtils.ordinalIndexOf(o1, "\t", 2));
                    String secondKey2 = o2.substring(StringUtils.ordinalIndexOf(o2, "\t", 1)+1, StringUtils.ordinalIndexOf(o2, "\t", 2));
                    return Integer.parseInt(secondKey1) - Integer.parseInt(secondKey2); //2nd key by 2nd column (chrom start) as int
                }
            }).collect(Collectors.toList());
            BufferedWriter writerTemp = Files.newBufferedWriter(tempSortedBedFile.toPath());
            BufferedWriter writerProBed = Files.newBufferedWriter(sortedProBed.toPath());
            sortedLines.stream().forEachOrdered(s -> {
                try {
                    writerTemp.write(s);
                    writerTemp.newLine();
                    if (chromNames.contains(s.substring(0, s.indexOf('\t')))) {
                        writerProBed.write(s);
                        writerProBed.newLine();
                    } else {
                        logger.debug("Chromosome not present in chrom txt file:" + s);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writerTemp.close();
            writerProBed.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Files.move(tempSortedBedFile.toPath(), inputProBed.toPath(), REPLACE_EXISTING);
        logger.info("Sorted new proBed fle: " + sortedProBed.getAbsolutePath());
        return sortedProBed;
    }

    public static File convertProBedToBigBed(File aSQL, String bedColumnsType, File sortedProBed, File inputChromSizes, File bigBedConverter) throws IOException, InterruptedException, URISyntaxException{
       logger.info("convertProBedToBigBed: " + aSQL.getAbsolutePath() +" , " + sortedProBed.getAbsolutePath() + ", " + inputChromSizes.getAbsolutePath());
        File result;
        final String OS = System.getProperty("os.name").toLowerCase();
        logger.info("OS version: " + OS);
        if (!OS.contains("win")) {
            InputStream inputStream = MzTabBedConverter.class.getClassLoader().getResourceAsStream("bedBigBed.sh");
            if (inputStream == null) {
                logger.error("no file for bedBigBed.sh found!");
                throw new IOException("no file for bedBigBed.sh found!");
            }
            File bedToBigBed = File.createTempFile("bigBedHelper", ".sh");
            FileUtils.copyInputStreamToFile(inputStream, bedToBigBed);
            logger.info("Created temp bedToBigBed script: " + bedToBigBed.toPath());
            bedToBigBed.setExecutable(true);
            result = new File(sortedProBed.getParentFile().getPath() + File.separator + FilenameUtils.getBaseName(sortedProBed.getName()) + ".bb");
            logger.info("command to run: \n" +
                    bigBedConverter.getPath() + ", " +
                            "-as=\"" + aSQL.getPath() + "\"" + ", " +
                            "-type=\"" + bedColumnsType + "\"" + ", " +
                            "-tab"+ ", " +
                    sortedProBed.getPath()+ ", " +
                    inputChromSizes.getPath()+ ", " +
                    result.getPath());
            Process bigbed_proc = new ProcessBuilder(
                    bedToBigBed.getAbsoluteFile().getAbsolutePath(),
                    bigBedConverter.getAbsoluteFile().getAbsolutePath(),
                    aSQL.getAbsoluteFile().getAbsolutePath(),
                    bedColumnsType,
                    sortedProBed.getAbsolutePath(),
                    inputChromSizes.getAbsoluteFile().getAbsolutePath(),
                    result.getPath())
                    .redirectErrorStream(true)
                    .start();
            BufferedReader in = new BufferedReader(new InputStreamReader(bigbed_proc.getInputStream()));
            String scriptOutput;
            while ((scriptOutput = in.readLine()) != null) {
                logger.info(scriptOutput);
                logger.info("Output System message: " + scriptOutput);
            }
            bigbed_proc.waitFor();
            in.close();
            logger.info("Finished generating bigBed file: " + result.getPath());
            File sortedTempFile = new File(sortedProBed.getParentFile().getPath() + File.separator + FilenameUtils.getBaseName(sortedProBed.getName()) + "_temp");
            BufferedReader reader = Files.newBufferedReader(sortedProBed.toPath(), Charset.defaultCharset());
            BufferedWriter writer = Files.newBufferedWriter(sortedTempFile.toPath(), Charset.defaultCharset());
            writer.write("# proBed-version\t" + PROBED_VERSION);
            writer.newLine();
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line, 0, line.length());
                writer.newLine();
            }
            reader.close();
            writer.close();
            sortedProBed.delete();
            Files.move(sortedTempFile.toPath(), sortedProBed.toPath());
            logger.info("Added proBed version number to the sorted proBed File.");
        } else {
            final String MESSAGE = "Unable to convert to bigBed on the Windows platform.";
            logger.error(MESSAGE);
            throw new IOException(MESSAGE);
        }
        logger.info("returning: " + result.getAbsolutePath());
        return result;
    }

    public static void createAsql(String name, String path) throws IOException{
        SimpleDateFormat df = new SimpleDateFormat("yyyymmdd");
        final String DATE = df.format(new Date());
        String text = "table ProteoAnnotator_reanalysis_" + name + "_" + DATE + "\n" +
                "\"" + "ProteoAnnotator reanalysis of " + name + " " + DATE + "\"\n" +
                "(\n" +
                "string  chrom;          \"The reference sequence chromosome.\"\n" +
                "uint    chromStart;     \"The position of the first DNA base.\"\n" +
                "uint    chromEnd;       \"The position of the last DNA base.\"\n" +
                "string  name;           \"Unique name for the BED line.\"\n" +
                "uint    score; \"A score used for shading by visualisation software.\"\n" +
                "char[1]  strand;                \"The strand.\"\n" +
                "uint    thickStart ;     \"Start position of feature on chromosome.\"\n" +
                "uint    thickEnd ;       \"End position of feature on chromosome.\"\n" +
                "uint    reserved  ; \"Reserved.\" \n" +
                "int  blockCount ; \"The number of blocks (exons) in the BED line.\"\n" +
                "int[blockCount] blockSizes ; \"A comma-separated list of the block sizes.\"\n" +
                "int[blockCount] chromStarts ; \"A comma-separated list of block starts.\"\n" +
                "string  proteinAccession ; \"The accession number of the protein.\"\n" +
                "string  peptideSequence; \"The peptide sequence.\"\n" +
                "string  uniqueness; \"The uniqueness of the peptide in the context of the genome sequence.\"\n" +
                "string  genomeReferenceVersion ; \"The genome reference version number\"\n" +
                "string  psmScore; \"One representative PSM score.\"\n" +
                "string  fdr; \"A cross-platform measure of the likelihood of the identification being incorrect.\"\n" +
                "string  modifications; \"Semicolon-separated list of modifications identified on the peptide.”\n" +
                "string  charge; \"The value of the charge.\"\n" +
                "string  expMassToCharge; \"The value of the experimental mass to charge.\"\n" +
                "string  calcMassToCharge; \"The value of the calculated mass to charge.\"\n" +
                "string  datasetID;  \"A unique identifier or name for the data set.\"\n" +
                "string  psmRank;  \"The rank of the PSM.\"\n" +
                "string  uri; \"A URI pointing to the file's source data.\"\n" +
                ")";
        Files.write(Paths.get(path), text.getBytes());
        logger.info("Finished creating new aSQL file: " + path);
    }

}
