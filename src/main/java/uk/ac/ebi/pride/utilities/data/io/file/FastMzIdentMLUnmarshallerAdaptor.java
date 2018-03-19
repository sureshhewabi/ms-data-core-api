package uk.ac.ebi.pride.utilities.data.io.file;

import uk.ac.ebi.pride.utilities.data.lightModel.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Suresh Hewapathirana
 */
public class FastMzIdentMLUnmarshallerAdaptor {

    private MzIdentML mzIdentML = null;

    public FastMzIdentMLUnmarshallerAdaptor(File mzIdentMLFile) {
        FastMzIdentMLUnmarshaller fastMzIdentMLUnmarshaller = FastMzIdentMLUnmarshaller.getInstance(mzIdentMLFile);
        this.mzIdentML = fastMzIdentMLUnmarshaller.getMzIdentML();

    }

    public MzIdentML getMzIdentML() {
        return mzIdentML;
    }

    public Collection<Comparable> getProteinIds() {
        return mzIdentML.getSequenceCollection().getDBSequence()
                .parallelStream()
                .map(DBSequence::getId)
                .collect(Collectors.toList());
    }

    public Collection<Comparable> getPeptideIds() {
        return mzIdentML.getSequenceCollection().getPeptide()
                .parallelStream()
                .map(Peptide::getId)
                .distinct()
                .collect(Collectors.toList());
    }

    public Peptide getPeptideById(Comparable peptideRef) {
        return mzIdentML.getSequenceCollection().getPeptide()
                .stream()
                .filter(p -> p.getId().equals(peptideRef))
                .findFirst()
                .orElse(null);
    }

    public List<SpectrumIdentificationList> getSpectrumIdentificationList() {
        return mzIdentML.getDataCollection().getAnalysisData().getSpectrumIdentificationList();
    }

    public List<SpectrumIdentificationResult> getSpectrumIdentificationResultByIndex(int index) {
        SpectrumIdentificationList spectrumIdentificationList = getSpectrumIdentificationList().get(index);
        return (getSpectrumIdentificationList().isEmpty() || getSpectrumIdentificationList() == null) ? null : spectrumIdentificationList.getSpectrumIdentificationResult();
    }

    /**
     * <DataCollection>
     * <Inputs>
     * <SpectraData location="file:///lolo//small.mgf" id="SD_1">
     * <FileFormat>
     * <cvParam accession="MS:1001062" name="Mascot MGF file" cvRef="PSI-MS" />
     * </FileFormat>
     * <SpectrumIDFormat>
     * <cvParam accession="MS:1000774" name="multiple peak list nativeID format" cvRef="PSI-MS" />
     * </SpectrumIDFormat>
     * </SpectraData>
     * </Inputs>
     * </DataCollection>
     * <p>
     * This method extract the Inputs from the MzIdentML object and
     * for each input file, it retrieves the Spectra Data such as
     * file location, file format etc.
     *
     * @return Map<Comparable   ,       SpectraData> where Comparable will be the Id of the SpectraData
     */
    public Map<Comparable, SpectraData> getSpectraDataMap() {
        Inputs inputs = mzIdentML.getDataCollection().getInputs();
        List<SpectraData> spectraDataList = inputs.getSpectraData();
        Map<Comparable, SpectraData> spectraDataMap = null;
        if (spectraDataList != null && spectraDataList.size() > 0) {
            spectraDataMap = new HashMap<>();
            for (SpectraData spectraData : spectraDataList) {
                spectraDataMap.put(spectraData.getId(), spectraData);
            }
        }
        return spectraDataMap;
    }

    /**
     * Close data access controller by clearing the entire mzIdentML Object
     */
    public void close() {
        mzIdentML = null;
    }
}
