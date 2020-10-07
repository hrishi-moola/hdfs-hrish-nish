package edu.usfca.cs.chat.Utils;

import com.google.protobuf.ByteString;
import edu.usfca.cs.chat.DfsMessages;
import edu.usfca.cs.chat.net.MessagePipeline;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class FileUtils {

    public static void writeToFile(DfsMessages.FileChunk fileChunk, String storagePath, String type) throws Exception{
        String fileName = fileChunk.getFilepath();

        String directoryName = fileName.split("-")[0];
        String chunkNum = fileName.split("-")[1];
        System.out.println("directoryName = " + directoryName);
        System.out.println(storagePath + type  + directoryName + "/chunk-" + chunkNum);

        Path path = Paths.get(storagePath + type + directoryName);
        Files.createDirectories(path);

        path = Paths.get(storagePath + type  + directoryName + "/chunk-" + chunkNum);
        Files.write(path, fileChunk.getChunks().toByteArray());

        path = Paths.get(storagePath + type  + directoryName + "/chunk-" + chunkNum+ "_checksum");
        Files.write(path, getCRC32Checksum(fileChunk.getChunks().toByteArray()));
    }

    public static void storeFile(String fileName, byte[] fileContents) throws IOException {
        Path path = Paths.get(fileName);
        Files.write(path, fileContents);
    }
    public static byte[] getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(crc32.getValue()).array();
    }

    private static void purgeDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory())
                purgeDirectory(file);
            file.delete();
        }
    }

    public static File findDir(String name, File file) {
        File[] list = file.listFiles();
        if(list!=null)
            for (File innerFile : list)
            {
                if (name.equals(innerFile.getName())) {
                    return innerFile;
                }
                else if (innerFile.isDirectory())
                {
                    return findDir(name,innerFile);
                }
                else return null;
            }
        return null;
    }

    public static void clearDirectoryContents(String storagePath) {
        try {
            File dir = new File(storagePath);
            purgeDirectory(dir);
        }
        catch (Exception e) {
            System.out.println(e + " Error clearing storage node contents before startup");
        }
    }

    public static DfsMessages.FileChunk getChunks(File filepath, DfsMessages.FileChunkHeader fileChunkHeader) throws IOException {

        byte[] fileChunkString = Files.readAllBytes(Paths.get(filepath.getCanonicalPath()));
        byte[]  checkSum = Files.readAllBytes(Paths.get(filepath.getCanonicalPath() + "_checksum"));
        if(Arrays.equals(getCRC32Checksum(ByteString.copyFrom(fileChunkString).toByteArray()), checkSum)){
            System.out.println("issokay");
            return DfsMessages.FileChunk.newBuilder().setFilepath(filepath.getName()).setChunks(ByteString.copyFrom(fileChunkString)).setFilechunkHeader(fileChunkHeader).build();
        } else { //request replica
            System.out.println("Time to request replicas");
        }
        return null;
    }


    public static DfsMessages.FileRequest getFileRequest(String localFile, String dfsPath, double chunkSize){
        File f = new File(localFile);
        return DfsMessages.FileRequest.newBuilder()
                .setFilepath(dfsPath).setType(DfsMessages.FileRequest.Type.STORE)
                .setDirectory(localFile)
                .setNumChunks(new Double(Math.ceil(f.length() / chunkSize)).intValue()).setSize(f.length()).build();
    }
    public static void main(String[] args) throws Exception{

        String storageDirectory = "storage1/original/";
        String file = "nato_trial_file.txt";
        File dir = new File(storageDirectory + file);
//        List<DfsMessages.FileChunk> chunks = new ArrayList<>();
//        for (File eachFile: dir.listFiles()) {
////            if (!eachFile.getName().contains("checksum") &&  !eachFile.getName().contains("metadata"))
////                chunks.add(getChunks(eachFile, fileChunkMetadataMap.get(filepath)));
//        }

        System.out.println(getFileName("dfs/nato1"));
//        System.out.println(chunks);
    }

    public static String getDirPath(String filepath) {
        System.out.println("filepath: " + filepath);
        int fileNameStart = filepath.lastIndexOf('/');
        System.out.println("fileNameStart: " + fileNameStart);

        if(fileNameStart == -1) return "";
        return filepath.substring(0, fileNameStart);
    }

    public static String getFileName(String filepath) {
        int fileNameEnd = filepath.lastIndexOf('/');
        if(fileNameEnd == -1) return "";
        return filepath.substring(fileNameEnd + 1);
    }

    public static boolean doesFileExist(String filepath) {
        System.out.println(new File(filepath).isFile());
        return new File(filepath).isFile();
    }
}
