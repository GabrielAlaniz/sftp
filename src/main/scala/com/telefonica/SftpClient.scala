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
        log.info("Conexão ao servidor SFTP com sucesso")
      }
    } catch{
      case e: Throwable => 
      log.error(("Erro ao tentar conectar com o servidor SFTP"))
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
    	  log.info("Lista de arquivos obtidos com sucesso")
    	  dir.forEach(println)
      }
    } catch {
      case e: Throwable => log.error("Erro no path: "+e.getMessage)
    }
  }
  
  def downloadDesdeSftp(sftp: ChannelSftp, localPath: String, remotePath: String){
    try{
    	if(sftp.isConnected()){
    	  getRemoteFile(sftp, localPath, remotePath)
      }
    } catch {
      case e: Throwable => log.error("Erro no path: "+e.getMessage)
    } 
    finally {
      disconnect(sftp)
    }
  }
  
  def uploadDesdeLocal(sftp: ChannelSftp, localDir: String, remotePath: String){
    try{
      if(sftp.isConnected()){
        putLocalFile(sftp, localDir, remotePath)
      }
    } catch {
      case e: Throwable => log.error("Erro no path: "+e.getMessage)
    }
    finally {
      disconnect(sftp)
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
    	        log.info(s"Transferência de arquivo ${entry.getFilename} para o diretório ${localPath}")
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
            sftp.cd(remotePath)
            println(entry.getFilename+" - "+remotePath+entry.getFilename+" - "+localDir)
            sftp.put(sftp.lpwd()+"/"+entry.getFilename, remotePath+entry.getFilename)
            log.info(s"Transferência de arquivo ${entry.getFilename} para o diretório SFTP: ${remotePath}")
          } else if(entry.getAttrs.isDir()){
          	var newLocalPath = localDir+entry.getFilename+"/"
          	sftp.lcd(newLocalPath)
          	println(newLocalPath)
            var newRemotePath = remotePath+entry.getFilename+"/"
            new File(newRemotePath).mkdir()
//            val perm = "777"
//            sftp.chmod(Integer.parseInt(perm,8), remotePath)
//            sftp.chmod(Integer.parseInt(perm,8), localDir)
            println("Criada: "+newRemotePath)
            putLocalFile(sftp, newLocalPath, newRemotePath)
          }
        }
      )
    } catch {
      case e: Throwable => e.printStackTrace()
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