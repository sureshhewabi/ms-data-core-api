package uk.ac.ebi.pride.utilities.data.io.file;

import uk.ac.ebi.pride.utilities.data.utils.MzTabUtils;
import uk.ac.ebi.pride.jmztab.model.*;
import uk.ac.ebi.pride.jmztab.utils.MZTabFileParser;
import uk.ac.ebi.pride.utilities.term.QuantCvTermReference;
import uk.ac.ebi.pride.utilities.util.NumberUtilities;
import uk.ac.ebi.pride.utilities.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;


/**
 * Unmarshaller an MZTab file
 * @author ypriverol
 * @author rwang
 */
public class MzTabUnmarshallerAdaptor extends MZTabFileParser{

    //Todo: mzTab do not have a way to retrieve for each Protein the PSM of peptides

    Map<Comparable, List<Comparable>> proteinPSMMap;

    private static Integer NUMBER_PROTEIN_LOOP = 10;


    private int numIdentifiedPeptides;
    private Map<Integer, Peptide> peptides;

    public MzTabUnmarshallerAdaptor(File tabFile, OutputStream out) throws IOException {
        super(tabFile, out);

        proteinPSMMap = new HashMap<Comparable, List<Comparable>>();
    }

    public Map<Integer,MsRun> getSourceFiles(){
        return this.getMZTabFile().getMetadata().getMsRunMap();
    }

    public Map<Integer, Contact> getContacts() {
        return getMZTabFile().getMetadata().getContactMap();
    }

    public Map<Integer, Sample> getSamples() {
        return getMZTabFile().getMetadata().getSampleMap();
    }

    public Map<Integer, Assay> getAssays(){
        return getMZTabFile().getMetadata().getAssayMap();
    }

    public Map<Integer, StudyVariable> getStudyVariables(){
        return getMZTabFile().getMetadata().getStudyVariableMap();
    }

    public Map<Integer, Software> getDataSoftwares() {
        return getMZTabFile().getMetadata().getSoftwareMap();
    }

    /**
     * Retrieve all publications from mzTab file
     * @return a Map of Reference from mzTab File
     */
    public Map<Integer, Publication> getReferences() {
        return getMZTabFile().getMetadata().getPublicationMap();
    }

    /**
     * Retrieve a general description from the file
     * @return Retrieve the mzTab CvParams
     */
    public List<Param> getAdditionalParams(){
        List<Param> listParams = getMZTabFile().getMetadata().getCustomList();
        Param paramQuant = getMZTabFile().getMetadata().getQuantificationMethod();
        if(paramQuant != null)
            listParams.add(paramQuant);
        paramQuant = getMZTabFile().getMetadata().getProteinQuantificationUnit();
        if(paramQuant != null)
            listParams.add(paramQuant);
        paramQuant = getMZTabFile().getMetadata().getPeptideQuantificationUnit();
        if(paramQuant != null)
            listParams.add(paramQuant);

        return listParams;
    }

    public String getExpTitle() {
        return getMZTabFile().getMetadata().getTitle();
    }

    public String getExpAccession() {
        return getMZTabFile().getMetadata().getMZTabID();
    }

    public String getVersion() {
        return getMZTabFile().getMetadata().getMZTabVersion();
    }

    public int getNumberOfPeptides(int rank) {
        return 0;
    }

    public boolean hasProteinSequence() {
        return getMZTabFile().getProteinColumnFactory().isOptionalColumn(MzTabUtils.OPTIONAL_SEQUENCE_COLUMN);
    }

    public Map<Integer, MsRun> getMRunMap() {
        return getMZTabFile().getMetadata().getMsRunMap();
    }

    public Map<Integer, List<String>> getPSMtoMsRunMap() {
        Map<Integer, List<String>> psmIds = new HashMap<Integer, List<String>>();
        Iterator<PSM> psmIterator = getMZTabFile().getPSMs().iterator();
        while(psmIterator.hasNext()){
            PSM psm = psmIterator.next();
            SplitList<SpectraRef> refs = psm.getSpectraRef();
            for(SpectraRef ref:refs){
                MsRun msRun = ref.getMsRun();
                if(psmIds.containsKey(msRun.getId()))
                    psmIds.get(msRun.getId()).add(psm.getPSM_ID());
                else{
                    List<String> psmIDs = new ArrayList<String>();
                    psmIDs.add(psm.getPSM_ID());
                    psmIds.put(msRun.getId(), psmIDs);
                }
            }
        }
        return psmIds;
    }

    public Map<Integer, PSM> getPSMs() {
        return getMZTabFile().getPSMsWithLineNumber();
    }
    //Todo: The concept of ambiguity members in mzTab is more simple that the concept of mzIdentML for that reason the
    //Todo: present version of ms-data-core-api do not handle this concept as ProteinAmbiguity
    //
    public boolean hasProteinGroup() {
        return false;
    }

    public List<String> getProteinGroupIds() {
        List<String> accessions = new ArrayList<String>();
        Collection<Protein> proteins = getMZTabFile().getProteins();
        for(Protein protein: proteins)
          accessions.add(protein.getAccession());
        return accessions;
    }

    /**
     * Retrieve the Map of proteins with the corresponding list of PSMs for each protein.
     * //Todo: We need to figure it out How the peptides will be included in the near future. Also some protein Ids included in the file
     * // Todo: will be missing because the reference for them do not exist.
     * @return
     */
    public Tuple<Map<String, List<String>>, Map<String, List<String>>> getAllProteinAccessions() {

        Map<String, List<String>> proteinPSMsIds = new HashMap<String, List<String>>();
        Map<String, List<String>> proteinPeptideIds = new HashMap<String, List<String>>();
        Map<Integer, Protein> proteinMap = getMZTabFile().getProteinsWithLineNumber();
        for(Map.Entry proteinEntry: proteinMap.entrySet()){
            String proteinId = proteinEntry.getKey().toString();
            Protein protein   = (Protein) proteinEntry.getValue();

            for(Map.Entry psmEntry: getMZTabFile().getPSMsWithLineNumber().entrySet()){
                Integer psmId = (Integer) psmEntry.getKey();
                PSM psm       = (PSM) psmEntry.getValue();
                List<String> psmIds = new ArrayList<String>();
                if(psm.getAccession().equalsIgnoreCase(protein.getAccession())){
                    if(proteinPSMsIds.containsKey(proteinId))
                        psmIds = proteinPSMsIds.get(proteinId);
                    psmIds.add(psmId.toString());
                    proteinPSMsIds.put(proteinId,psmIds);
                }
            }
            for(Map.Entry psmEntry: getMZTabFile().getPeptidesWithLineNumber().entrySet()){
                Integer psmId = (Integer) psmEntry.getKey();
                Peptide psm       = (Peptide) psmEntry.getValue();
                List<String> psmIds = new ArrayList<String>();
                if(psm.getAccession().equalsIgnoreCase(protein.getAccession())){
                    if(proteinPeptideIds.containsKey(proteinId))
                        psmIds = proteinPeptideIds.get(proteinId);
                    psmIds.add(psmId.toString());
                    proteinPeptideIds.put(proteinId,psmIds);
                }
            }

        }
        return new Tuple<Map<String, List<String>>, Map<String, List<String>>>(proteinPeptideIds, proteinPSMsIds);
    }

    public Map<Integer, Instrument> getInstrument() {
        return getMZTabFile().getMetadata().getInstrumentMap();
    }

    public Set<String[]> getDatabases() {
        Set<String[]> databases = new HashSet<String[]>();
        Iterator<Protein> proteinIterator = getMZTabFile().getProteinsWithLineNumber().values().iterator();
        int countLoop = 0;
        while(proteinIterator.hasNext() && countLoop < NUMBER_PROTEIN_LOOP){
           Protein protein = proteinIterator.next();
            if(protein.getDatabase() != null){
                String[] database = new String[2];
                database[0] = protein.getDatabase();
                database[1] = protein.getDatabaseVersion();
                databases.add(database);
            }
        }
        return databases;
    }

    public SortedMap<Integer, SplitList<Param>> getProtocol() {
        return getMZTabFile().getMetadata().getSampleProcessingMap();
    }

    /**
     * It would look if the protein is in the file and retrieve the data, if the accession is not in the file with information
     * it would create an empty entry Protein for it.
     * @param proteinId
     * @return
     */
    public Tuple<Integer, Protein> getProteinById(Comparable proteinId) {
        if(proteinId != null && NumberUtilities.isInteger(proteinId.toString()))
            return new Tuple<Integer, Protein>(Integer.parseInt(proteinId.toString()), getMZTabFile().getProteinsWithLineNumber().get(Integer.parseInt(proteinId.toString())));
        return null;
    }

    public Map<Integer, PSM> getSpectrumIdentificationsByIds(List<Comparable> spectrumIdentIds) {
        Map<Integer, PSM> psmList = new HashMap<Integer, PSM>();
        for(Comparable id: spectrumIdentIds){
           if(id != null && NumberUtilities.isInteger(id.toString())){
               PSM psm = getMZTabFile().getPSMsWithLineNumber().get(Integer.parseInt(id.toString()));
               if(psm != null)
                   psmList.put(Integer.parseInt(id.toString()), psm);
           }
        }
        return psmList;
    }

    public Metadata getMetadata(){
        return getMZTabFile().getMetadata();
    }

    public int getNumIdentifiedPeptides() {
        return getMZTabFile().getPSMsWithLineNumber().size();
    }

    public boolean hasQuantitationData() {
        if(getMZTabFile().getMetadata().getMZTabType() == MZTabDescription.Type.Quantification){
            Param method = getMetadata().getQuantificationMethod();
            return (QuantCvTermReference.getQuantitativeMethodParam(method.getAccession())!= null);
        }
        return false;
    }

    public Map<Integer, Peptide> getPeptides() {
        return getMZTabFile().getPeptidesWithLineNumber();
    }


    public Map<Integer, Peptide> getPeptideByIds(List<Comparable> peptideIds) {
       Map<Integer, Peptide> peptides = new HashMap<Integer, Peptide>();
        for(Comparable id: peptideIds){
            if(id != null && NumberUtilities.isInteger(id.toString())){
                Peptide psm = getMZTabFile().getPeptidesWithLineNumber().get(Integer.parseInt(id.toString()));
                if(psm != null)
                    peptides.put(Integer.parseInt(id.toString()), psm);
            }
        }
        return peptides;
    }
}
