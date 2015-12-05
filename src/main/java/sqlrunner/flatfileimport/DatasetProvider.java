package sqlrunner.flatfileimport;

import java.io.File;

public interface DatasetProvider {

    long retrieveDatasetCount() throws Exception;

    void setupDatasetProvider(File file, boolean testMode, ImportAttributes properties) throws Exception;
    
    Object getNextDataset() throws Exception;

    Object getDatasetAtRowInTestMode(long rowNumber) throws Exception;

    void closeDatasetProvider();
    
    FieldTokenizer createParser();
    
    long getCurrentRowNum();

}
