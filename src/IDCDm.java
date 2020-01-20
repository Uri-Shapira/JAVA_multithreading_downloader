import java.io.IOException;
import java.io.File;
import java.io.*;
import java.util.ArrayList;


public class IdcDm {

    public static void main(String[] args) throws ClassNotFoundException, InterruptedException, IOException {
        initializeDownloader(args);
//        test_metadata();
        }

    private static void initializeDownloader(String[] i_applicationInputs)throws ClassNotFoundException, InterruptedException{
        if(i_applicationInputs.length < 1 || i_applicationInputs.length > 2){
            System.err.println("Incorrect inputs given. IdcDm can only receive one or two inputs. Download failed!");
            System.exit(1);
        }
        DownloadManager manager;
        ArrayList<String> urlList;
        // The downloader will read from the url input stream in chunks of "chunkSize"
        // which corresponds to one index in the metadata array
        int chunkSize = 4096;
        if(new File(i_applicationInputs[0]).exists()){
            urlList = getUrlList(i_applicationInputs[0]);
        }
        else{
            urlList = new ArrayList<>();
            urlList.add(i_applicationInputs[0]);
        }
        if(i_applicationInputs.length == 2){
        manager = new DownloadManager(urlList, Integer.parseInt(i_applicationInputs[1]),chunkSize);
        }
        else{
            manager = new DownloadManager(urlList, chunkSize);
        }
        manager.downloadFile();
    }

    private static ArrayList<String> getUrlList(String file){
        ArrayList<String> urlList = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line;
            while((line = reader.readLine()) != null){
                urlList.add(line);
            }
        }
        catch (IOException e){
            System.err.println("Failed to read the URL file " + file + ". Download failed!");
            System.exit(1);
        }
        return urlList;
    }

    private static void test_metadata() throws IOException, ClassNotFoundException{
        String file = "C:\\Users\\URISHAP\\Desktop\\netoworks_lab\\CentOS-6.10-x86_64-netinstall_metadata";
        FileInputStream fis = new FileInputStream(new File(file));
        ObjectInputStream ois = new ObjectInputStream(fis);
        Metadata status = (Metadata) ois.readObject();
        BufferedWriter writer = new BufferedWriter(new FileWriter("TEST_FILE_2"));
        for(int i = 0; i < status.chunksDownloaded.length; i++){
            long current = i * 4096;
            writer.write("chunk " + current + " downloaded: " + status.chunksDownloaded[i] + "\n");
        }
        System.out.println("total status size " + status.chunksDownloaded.length);
        ois.close();
        writer.flush();
        writer.close();
    }

}

/* TODO:

 *1. Add decision about whether to go for maximum concurrent connections or less based on file size
 * 5. throws classNotFound exception - not good!
 */
