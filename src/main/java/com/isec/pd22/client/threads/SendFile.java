package com.isec.pd22.client.threads;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.Request.FileUpload;
import com.isec.pd22.server.models.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SendFile extends Thread{

    ModelManager modelManager;
    File file;

    public SendFile(ModelManager modelManager, File file) {
        this.modelManager = modelManager;
        this.file = file;
    }

    @Override
    public void run() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            modelManager.notErrorUpdatingFile();
            System.out.println(e);
            return;
        }
        User u = modelManager.getUser();
        while (true){
            int sizeBytes;
            FileUpload fileUpload = new FileUpload(ClientActions.ADD_SPECTACLE);
            fileUpload.setUser(u);
            byte [] bytes = new byte[4000];
            try {
                sizeBytes = fis.read(bytes);
                fileUpload.setLast(sizeBytes == -1);
                fileUpload.setBytes(bytes);
                fileUpload.setSizeBytes(sizeBytes);
                fileUpload.setName(file.getName());
                modelManager.sendMessage(fileUpload);
                System.out.println("Foram enviados: " + sizeBytes);
                if(sizeBytes == -1){
                    System.out.println("File send successfully");
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
