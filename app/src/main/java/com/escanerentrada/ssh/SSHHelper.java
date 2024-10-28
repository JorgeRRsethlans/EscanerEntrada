package com.escanerentrada.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Clase que se encarga de manejar las operaciones SSH.
 */
public class SSHHelper {

    private final String username;
    private final String password;
    private final String host;
    private final int port;

    /**
     * Constructor de la clase.
     *
     * @param username Nombre de usuario
     * @param password Contraseña
     * @param host Nombre del host
     * @param port Puerto
     */
    public SSHHelper(String username , String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    /**
     * Método que se ejecuta para crear una carpeta en el servidor.
     *
     * @param remoteBasePath Ruta base de la carpeta
     * @return Ruta de la carpeta
     * @throws Exception Excepción en caso de error
     */
    public String createRemoteFolders(String remoteBasePath) throws Exception {
        Session session = createSession();
        String remotePath = remoteBasePath + "/" +
                findNextAvailableFolderNumber(remoteBasePath, session);
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("mkdir -p \"" + remotePath + "\"");
        channel.connect();
        channel.disconnect();
        session.disconnect();
        return remotePath;
    }

    /**
     * Método que se ejecuta para subir un archivo a un directorio en el servidor.
     *
     * @param localFilePath Ruta local del archivo
     * @param remoteFolderPath Ruta remota del directorio
     * @throws Exception Excepción en caso de error
     */
    public void uploadFile(String localFilePath, String remoteFolderPath) throws Exception {
        Session session = createSession();
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put(localFilePath, remoteFolderPath + "/etiqueta.pdf");
        sftpChannel.disconnect();
        session.disconnect();
    }

    /**
     * Método que se ejecuta para saber que carpeta crear en el servidor.
     *
     * @param remoteBasePath Ruta base de la carpeta
     * @param session Sesión SSH
     * @return Número de carpeta a crear
     * @throws Exception Excepción en caso de error
     */
    private int findNextAvailableFolderNumber(String remoteBasePath, Session session)
            throws Exception {
        int counter = 1;
        while(checkIfFolderExists(remoteBasePath + "/" + counter, session)) {
            counter++;
        }
        return counter;
    }

    /**
     * Método que se ejecuta para verificar si una carpeta existe en el servidor.
     *
     * @param fullFolderPath Ruta completa de la carpeta
     * @param session Sesión SSH
     * @return True si la carpeta existe, false en caso contrario
     * @throws Exception Excepción en caso de error
     */
    private boolean checkIfFolderExists(String fullFolderPath, Session session) throws Exception {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("[ -d \"" + fullFolderPath + "\" ] && echo \"exists\"");
        channel.setInputStream(null);

        InputStream in = channel.getInputStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().equals("exists")) {
                channel.disconnect();
                return true;
            }
        }

        channel.disconnect();
        return false;
    }

    /**
     * Método que se ejecuta para subir una imagen a un directorio en el servidor.
     *
     * @param imageData Datos de la imagen
     * @param remoteFilePath Ruta remota de la imagen
     * @throws Exception Excepción en caso de error
     */
    public void uploadImage(byte[] imageData, String remoteFilePath) throws Exception {
        Session session = createSession();
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put(new ByteArrayInputStream(imageData), remoteFilePath);
        sftpChannel.disconnect();
        session.disconnect();
    }

    /**
     * Método que se ejecuta para crear una sesión SSH.
     *
     * @return Sesión SSH
     * @throws Exception Excepción en caso de error
     */
    private Session createSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }
}
