package com.telefonica

import com.jcraft.jsch.JSch
import com.jcraft.jsch.ChannelSftp
import java.util.ArrayList
import java.io.File
import org.apache.log4j.Logger
import java.io.FileInputStream

class SftpClient {
//  @transient
//  lazy val log: Logger = Logger.getLogger(getClass.getName)
  
  def setupConnection(username: String, password: String, host: String, port: String): ChannelSftp = {
    val jsch = new JSch()
    var sftp = new ChannelSftp()
    val jschSession = jsch.getSession(username,host, port.toInt)
    jschSession.setPassword(password)
    val config = new java.util.Properties()
    config.put("StrictHostKeyChecking", "no")
    jschSession.setConfig(config)
    try{
      jschSession.connect()
      if(jschSession.isConnected()){
        sftp = (jschSession.openChannel("sftp")).asInstanceOf[ChannelSftp]
        sftp.connect()
//        log.info("Conexão exitosa")
      } else {
       throw new Exception(s"Erro ao tentar conectar no SFTP") 
//       log.error("Nao e possivel connector ao servidor SFTP")
      }
    } catch{
      case e: Throwable => e.printStackTrace()
//      log.error("Erro ao tentar conectar no SFTP: " + e.getMessage)
    }
    return sftp
  }
  
  def listarArquivos(sftp:ChannelSftp, path:String){
    try{
      if(sftp.isConnected()){
    	  val files = sftp.ls(path)
			  var dir = new ArrayList[String]
    	  files.forEach(c=>
  	      if(!c.toString().endsWith(".") && !c.toString().endsWith("..")){
    	      var entry: ChannelSftp#LsEntry = c.asInstanceOf[ChannelSftp#LsEntry]
    	      if(!entry.getAttrs.isDir()){
    	        dir.add(path+entry.getFilename)
    	      } else if(entry.getAttrs.isDir()){
    	        listarArquivos(sftp,path+entry.getFilename+"/")
  	        }
  	      } 
    	  )
    	  dir.forEach(println)
      } else {
        throw new Exception(s"Erro ao tentar conectar no SFTP")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
//      log.error("Erro ao tentar conectar no SFTP: " + e.getMessage)
    }
  }
  
  def downloadDesdeSftp(sftp: ChannelSftp, localPath: String, remotePath: String){
    try{
    	if(sftp.isConnected()){
    	  getRemoteFile(sftp, localPath, remotePath)
      } else {
        throw new Exception(s"Erro ao tentar conectar no SFTP")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    } 
    finally {
      disconnect(sftp)
    }
  }
  
  def uploadDesdeLocal(sftp: ChannelSftp, localDir: String, remotePath: String){
    try{
      if(sftp.isConnected()){
        putLocalFile(sftp, localDir, remotePath)
      } else {
        throw new Exception(s"Erro ao tentar conectar no SFTP")
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
    finally {
      sftp.disconnect()
    }
  }
  
  def getRemoteFile(sftp: ChannelSftp, localPath: String, remotePath: String){
    val files = sftp.ls(remotePath)
    	  files.forEach(c =>
    	    if(!c.toString().endsWith(".")){
    	      var entry: ChannelSftp#LsEntry = c.asInstanceOf[ChannelSftp#LsEntry]
    	      if(!entry.getAttrs.isDir()){
    	        sftp.lcd(localPath)
    	        sftp.cd(remotePath)
    	        sftp.get(entry.getFilename, sftp.lpwd()+"/"+entry.getFilename)
    	      } else if(entry.getAttrs.isDir()){
    	        var newLocalPath = localPath+"/"+entry.getFilename
    	        new File(newLocalPath).mkdir()
    	        sftp.lcd(newLocalPath)
    	        var newRemotePath = remotePath+"/"+entry.getFilename
    	        sftp.cd(newRemotePath)
    	        getRemoteFile(sftp, newLocalPath, newRemotePath)
    	      }
    	    }
    	  )
  }
    
 
  def putLocalFile(sftp: ChannelSftp, localDir: String, remotePath: String){
    try{
      val files = sftp.ls(localDir)
      files.forEach(c =>
        if(!c.toString().endsWith(".")){
          var entry: ChannelSftp#LsEntry = c.asInstanceOf[ChannelSftp#LsEntry]
          if(!entry.getAttrs.isDir()){
            sftp.lcd(localDir)
            println(sftp.lpwd().toString())
            sftp.cd(remotePath)
            println(sftp.pwd().toString())
            sftp.put(entry.getFilename, sftp.pwd()+"/"+entry.getFilename)
          } else if(entry.getAttrs.isDir()){
            var newLocalDir = localDir+"/"+entry.getFilename
            var newRemotePath = remotePath+"/"+entry.getFilename
            new File(newRemotePath).mkdir()
            putLocalFile(sftp, newLocalDir, newRemotePath)
          }
        }
      )
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
  
  /*if(localFile.isDirectory()){
      sftp.mkdir(localFile.getName)
//      log.info(s"Pasta: ${localFile.getName} criada em: ${remotePath}")
      var newRemotePath = remotePath+"/"+localFile.getName 
      sftp.cd(remotePath)
      for(f <- localFile.listFiles()){
        putLocalFile(sftp, f, newRemotePath)
      }
    } else {
      sftp.put(localFile.asInstanceOf[String], localFile.getName)
//      log.info(s"Copiando arquivo: ${localFile.getName} para: ${remotePath}")
    }*/

  def disconnect(sftp: ChannelSftp){
    if(sftp.isConnected()) {
      sftp.disconnect()
      sftp.exit()
//      log.info(s"Desconexao do servidor SFTP com sucesso")
    }
  }
  
  
}