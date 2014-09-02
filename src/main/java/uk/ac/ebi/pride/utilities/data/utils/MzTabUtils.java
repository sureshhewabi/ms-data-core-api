package uk.ac.ebi.pride.utilities.data.utils;

import uk.ac.ebi.pride.utilities.data.core.CvParam;
import uk.ac.ebi.pride.utilities.data.core.Score;
import uk.ac.ebi.pride.utilities.data.core.SpectraData;
import uk.ac.ebi.pride.utilities.data.core.UserParam;
import uk.ac.ebi.pride.jmztab.model.*;
import uk.ac.ebi.pride.jmztab.utils.convert.SearchEngineScoreParam;
import uk.ac.ebi.pride.utilities.term.CvTermReference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ypriverol
 * @author rwang
 */
public class MzTabUtils {

    public static final String OPTIONAL_ID_COLUMN         = "mzidentml_original_ID";
    public static final String OPTIONAL_SEQUENCE_COLUMN   = "protein_sequence";
    public static final String OPTIONAL_DECOY_COLUMN      = "cv_MS:1002217_decoy_peptide";
    public static final String OPTIONAL_RANK_COLUMN       = "cv_PRIDE:0000091_rank";

    /**
     * This function takes the value of a CVParam and retrieve the corresponding CVParam
     * @param cvParam A CvParam
     * @return CVParam for mzTab
     */
    public static CVParam convertCvParamToCVParam(CvParam cvParam){
        if(cvParam != null)
         return new CVParam(cvParam.getCvLookupID(),cvParam.getAccession(),cvParam.getName(),cvParam.getValue());
        return null;
    }

    public static List<CvParam> convertParamToCvParam(List<Param> params){
        List<CvParam> cvParamList = new ArrayList<CvParam>();
        if(params != null && params.size() > 0){
            for(Param param: params)
                cvParamList.add(MzTabUtils.convertCVParamToCvParam(param));
        }
        return cvParamList;
    }

    public static List<UserParam> convertStringListToUserParam(List<String> settingList) {
        List<UserParam> userParamList = new ArrayList<UserParam>();
        if(settingList != null && settingList.size() > 0){
            for(String setting: settingList)
                userParamList.add(MzTabUtils.convertStringToUserParam(setting));
        }
        return userParamList;
    }

    public static UserParam convertStringToUserParam(String value) {
        if(value != null && !value.isEmpty()){
            return new UserParam(null,null,value,null,null,null);
        }
        return null;
    }

    /**
     * This function takes the value of a UserParam and returns a corresponding CVParam
     * @param param User Param
     * @return Return a CVParam
     */
    public static CVParam convertUserParamToCVParam(UserParam param) {
        return new CVParam(null, null, param.getName(), param.getValue());
    }

    /**
     * Get the Search Engines Scores for a Proteins or Peptide
     * @return List of SearchEngine Score params
     */
    public static List<SearchEngineScoreParam> getSearchEngineScoreTerm(Score score) {
        List<SearchEngineScoreParam> scores = new ArrayList<SearchEngineScoreParam>();
        if (score != null)
            for (CvTermReference term : score.getCvTermReferenceWithValues())
                if(SearchEngineScoreParam.getSearchEngineScoreParamByAccession(term.getAccession()) != null)
                    scores.add(SearchEngineScoreParam.getSearchEngineScoreParamByAccession(term.getAccession()));
        return scores;
    }

    public static void addOptionalColumnValue(MZTabRecord record, MZTabColumnFactory columnFactory, String name, String value) {

        String header = OptionColumn.getHeader(null, name);
        MZTabColumn column = columnFactory.findColumnByHeader(header);

        String logicalPosition;

        if (column == null) {
            logicalPosition =  columnFactory.addOptionalColumn(name, String.class);
        }
        else {
            logicalPosition = column.getLogicPosition();
        }

        record.setValue(logicalPosition, value);
    }


    public static Constants.SpecIdFormat getSpectraDataIdFormat(SpectraData spectraData) {
        CvParam specIdFormat = spectraData.getSpectrumIdFormat();
        return getSpectraDataIdFormat(specIdFormat.getAccession());
    }

    private static Constants.SpecIdFormat getSpectraDataIdFormat(String accession) {
        if (accession.equals("MS:1001528"))
            return Constants.SpecIdFormat.MASCOT_QUERY_NUM;
        if (accession.equals("MS:1000774"))
            return Constants.SpecIdFormat.MULTI_PEAK_LIST_NATIVE_ID;
        if (accession.equals("MS:1000775"))
            return Constants.SpecIdFormat.SINGLE_PEAK_LIST_NATIVE_ID;
        if (accession.equals("MS:1001530"))
            return Constants.SpecIdFormat.MZML_ID;
        if (accession.equals("MS:1000776"))
            return Constants.SpecIdFormat.SCAN_NUMBER_NATIVE_ID;
        if (accession.equals("MS:1000770"))
            return Constants.SpecIdFormat.WIFF_NATIVE_ID;
        if (accession.equals("MS:1000777"))
            return Constants.SpecIdFormat.MZDATA_ID;
        if(accession.equals(("MS:1000768")))
            return Constants.SpecIdFormat.SPECTRUM_NATIVE_ID;
        return Constants.SpecIdFormat.NONE;
    }

    public static Constants.SpecFileFormat getSpectraDataFormat(SpectraData spectraData) {
        uk.ac.ebi.pride.utilities.data.core.CvParam specFileFormat = spectraData.getFileFormat();
        if (specFileFormat != null) {
            if (specFileFormat.getAccession().equals("MS:1000613"))
                return Constants.SpecFileFormat.DTA;
            if (specFileFormat.getAccession().equals("MS:1001062"))
                return Constants.SpecFileFormat.MGF;
            if (specFileFormat.getAccession().equals("MS:1000565"))
                return Constants.SpecFileFormat.PKL;
            if (specFileFormat.getAccession().equals("MS:1000584") || specFileFormat.getAccession().equals("MS:1000562"))
                return Constants.SpecFileFormat.MZML;
            if (specFileFormat.getAccession().equals("MS:1000566"))
                return Constants.SpecFileFormat.MZXML;
        }
        return Constants.SpecFileFormat.NONE;
    }

    public static String getSpectrumId(SpectraData spectraData, String spectrumID) {

        Constants.SpecIdFormat fileIdFormat = getSpectraDataIdFormat(spectraData);

        if (fileIdFormat == Constants.SpecIdFormat.MASCOT_QUERY_NUM) {
            String rValueStr = spectrumID.replaceAll("query=", "");
            String id = null;
            if(rValueStr.matches(Constants.INTEGER)){
                id = Integer.toString(Integer.parseInt(rValueStr) + 1);
            }
            return id;
        } else if (fileIdFormat == Constants.SpecIdFormat.MULTI_PEAK_LIST_NATIVE_ID) {
            String rValueStr = spectrumID.replaceAll("index=", "");
            String id = null;
            if(rValueStr.matches(Constants.INTEGER)){
                id = Integer.toString(Integer.parseInt(rValueStr) + 1);
            }
            return id;
        } else if (fileIdFormat == Constants.SpecIdFormat.SINGLE_PEAK_LIST_NATIVE_ID) {
            return spectrumID.replaceAll("file=", "");
        } else if (fileIdFormat == Constants.SpecIdFormat.MZML_ID) {
            return spectrumID.replaceAll("mzMLid=", "");
        } else if (fileIdFormat == Constants.SpecIdFormat.SCAN_NUMBER_NATIVE_ID) {
            return spectrumID.replaceAll("scan=", "");
        } else {
            return spectrumID;
        }
    }

    public static CvParam convertCVParamToCvParam(Param param) {
        return new CvParam(param.getAccession(),param.getName(),param.getCvLabel(),param.getValue(),null,null,null);
    }


}
