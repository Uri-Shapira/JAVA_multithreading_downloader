import java.io.IOException;
import java.io.File;
import java.io.*;


public class IdcDm {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        DownloadManager manager;
        if(args.length == 2){
             manager = new DownloadManager(args[0], Integer.parseInt(args[1]));
        }
        else{
            manager = new DownloadManager(args[0]);
        }
        manager.downloadFile();
//        test_metadata();
    }

    private static void test_metadata() throws IOException, ClassNotFoundException{
        String file = "C:/Users/URISHAP/Desktop/netoworks_lab/Mario1_500_metadata";
        FileInputStream fis = new FileInputStream(new File(file));
        ObjectInputStream ois = new ObjectInputStream(fis);
        Metadata status = (Metadata) ois.readObject();
        BufferedWriter writer = new BufferedWriter(new FileWriter("TEST_FILE_2"));
        for(int i = 0; i < status.batchesDownloaded.length; i++){
            long current = i * 4096;
            writer.write("chunk " + current + " downloaded: " + status.batchesDownloaded[i] + "\n");
        }
        System.out.println("total status size " + status.batchesDownloaded.length);
        ois.close();
        writer.flush();
        writer.close();
    }
}

/* TODO:
 * 1. Add decision about whether to go for maximum concurrent connections or less based on file size
 * 4. if download "breaks" - continue from last spot (metadata file probably)
 * 5. add logic if max concurrent is not given as input
 * 7. input is a file with multiple urls - download from multiple servers (??????????)
 */
