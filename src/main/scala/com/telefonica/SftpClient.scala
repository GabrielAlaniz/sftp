package com.telefonica

import com.jcraft.jsch.JSch
import com.jcraft.jsch.ChannelSftp
import java.util.ArrayList
import java.io.File
import org.apache.log4j.Logger
import java.io.FileInputStream

class SftpClient {
  @transient
  lazy val log: Logger = Logger.getLogger(getClass.getName)
  
  def setupConnection(username: String, host: String, port: String): ChannelSftp = {
    val jsch = new JSch()
    var sftp = new ChannelSftp()
    val jschSession = jsch.getSession(username,host, port.asInstanceOf[Int])
    val config = new java.util.Properties()
    config.put("StrictHostKeyChecking", "no")
    jschSession.setConfig(config)
    jschSession.connect()
    if(jschSession.isConnected()){
      sftp = (jschSession.openChannel("sftp")).asInstanceOf[ChannelSftp]
      sftp.connect()
      log.info("Conexão exitosa")
    } else {
     throw new Exception(s"Erro ao tentar conectar no SFTP") 
    }
    return sftp
  }
  
  def listarArquivos(sftp:ChannelSftp, path:String){
    try{
      if(sftp.isConnected()){
    	  val files:Vector[ChannelSftp#LsEntry] = sftp.ls(path).asInstanceOf
			  var dir = new ArrayList[String]
			  for(f <- files){
				  if(!f.getAttrs.isDir()){
					  dir.add(path+"/"+f.getFilename) // guardo los archivos finales
				  } else if(f.getAttrs.isDir()){
					  listarArquivos(sftp, path+"/"+f.getFilename)
			    }
		    }  
      } else {
        throw new Exception(s"Erro ao tentar conectar no SFTP")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
      log.error("Erro ao tentar conectar no SFTP: " + e.getMessage)
    }
  }
  
  def downloadDesdeSftp(sftp: ChannelSftp, localPath: String, remotePath: String){
    try{
    	if(sftp.isConnected()){
      	var localDir:File = localPath.asInstanceOf[File]
        if(!localPath.asInstanceOf[File].isDirectory()){
          localDir.mkdir()
        }
        localDir = sftp.lcd(localPath).asInstanceOf[File]
        getRemoteFile(sftp, remotePath, localDir)
//        getRemoteFile(sftp, remotePath, localDir) // localDir: String
        
        disconnect(sftp)
      } else {
        throw new Exception(s"Erro ao tentar conectar no SFTP")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
  
  def uploadDesdeLocal(sftp: ChannelSftp, localFile: String, remotePath: String){
    try{
      if(sftp.isConnected()){
        putLocalFile(sftp, localFile.asInstanceOf[File], remotePath)
        
        disconnect(sftp)
      } else {
        throw new Exception(s"Erro ao tentar conectar no SFTP")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
  
  def getRemoteFile(sftp: ChannelSftp, remotePath: String, localDir: File){
//  def getRemoteFile(sftp: ChannelSftp, remotePath: String, localDir: String){
    val files:Vector[ChannelSftp#LsEntry] = sftp.ls(remotePath).asInstanceOf
    for(f <- files){
      if(!f.getAttrs.isDir() && !(new java.io.File(localDir+"/"+f.getFilename)).exists){
        sftp.get(f.getFilename.asInstanceOf[String], localDir.asInstanceOf[String])
      } else if(f.getAttrs.isDir()) {
        new File(localDir+"/"+f.getFilename).mkdir()
        log.info(s"Pasta: ${f.getFilename} criada em: ${localDir}")
        getRemoteFile(sftp, remotePath+"/"+f.getFilename, (localDir+"/"+f.getFilename).asInstanceOf[File])
//        getRemoteFile(sftp, remotePath+"/"+f.getFilename, localDir+"/"+f.getFilename)
      }
    }
  }
  
  def putLocalFile(sftp: ChannelSftp, localFile: File, remotePath: String){
    if(localFile.isDirectory()){
      sftp.mkdir(localFile.getName)
      log.info(s"Pasta: ${localFile.getName} criada em: ${remotePath}")
      var newRemotePath = remotePath+"/"+localFile.getName 
      sftp.cd(remotePath)
      for(f <- localFile.listFiles()){
        putLocalFile(sftp, f, newRemotePath)
      }
    } else {
      sftp.put(localFile.asInstanceOf[String], localFile.getName)
      log.info(s"Copiando arquivo: ${localFile.getName} para: ${remotePath}")
    }
  }
  
  def disconnect(sftp: ChannelSftp){
    if(sftp.isConnected()) {
      sftp.disconnect()
      sftp.exit()
      log.info(s"Desconexao do servidor SFTP com sucesso")
    }
  }
  
  
}