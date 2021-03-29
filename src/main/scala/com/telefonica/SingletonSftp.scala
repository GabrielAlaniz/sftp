package com.telefonica

import com.jcraft.jsch.JSch
import com.jcraft.jsch.ChannelSftp
import java.util.ArrayList
import java.io.File
import org.apache.log4j.Logger

//object Sftp{
//  def main(args:Array[String]){
//    //new SingletonSftp().listarArquivos("gabi", "123", "127.0.0.1", "22", "/home/gabi/ftp/")
//    //new SingletonSftp().downloadDesdeSftp("gabi", "123", "127.0.0.1", "22", "/home/hdp/Documents/directoriosftp/", "/home/gabi/ftp/")
//    //new SingletonSftp().uploadDesdeLocal("gabi", "123", "127.0.0.1", "22", "/home/hdp/Documents/directoriosftp/pasta1", "/home/gabi/ftp/")
//  }
//}

class SingletonSftp {
//  @transient lazy val log: Logger = Logger.getLogger(getClass.getName)

  def listarArquivos(username: String, password: String, host: String, port: String, path: String){
    val sftp = setupConnection(username, password, host, port)
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
    	    	  listarArquivos(username, password, host, port, path+entry.getFilename+"/")
    	      }
  	      } 
    	  )
//    	  log.info("Lista de arquivos obtidos com sucesso")
    	  dir.forEach(println)
      }
    } catch {
      case e: Throwable => e.getMessage 
        //log.error("Erro no path: "+e.getMessage)
    }
    finally {
      disconnect(sftp)
    }
  }

  def downloadDesdeSftp(username: String, password: String, host: String, port: String, localPath: String, remotePath: String){
    
    val sftp = setupConnection(username, password, host, port)
    
    try{
    	if(sftp.isConnected()){
    	  getRemoteFile(sftp, localPath, remotePath)
      }
    } catch {
      case e: Throwable => e.getMessage
      //log.error("Erro no path: "+e.getMessage)
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
    	  //log.info(s"Transferência de arquivo ${entry.getFilename} para o diretório ${localPath}")
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
  
  def uploadDesdeLocal(username: String, password: String, host: String, port: String, localDir: String, remotePath: String){
    
    val sftp = setupConnection(username, password, host, port)
    
    try{
      if(sftp.isConnected()){
        putLocalFile(sftp, localDir, remotePath)
      }
    } catch {
      case e: Throwable => e.getMessage
      //log.error("Erro no path: "+e.getMessage)
    }
    finally {
      disconnect(sftp)
    }
  }
  
  def putLocalFile(sftp: ChannelSftp, localDir: String, remotePath: String){
    try{
      var local = new File(localDir)
      var remote = remotePath
      if(local.isDirectory()){
        remote = remote+"/"+local.getName
        sftp.mkdir(remote)
        sftp.cd(remote)
        //log.info(s"Pasta criada: ${local.getName}")
        var dir = sftp.ls(local.toString())
        dir.forEach(c =>
          if(!c.toString().endsWith(".")){
            var entry: ChannelSftp#LsEntry = c.asInstanceOf[ChannelSftp#LsEntry]
            if(!entry.getAttrs.isDir()){
              sftp.put(local+"/"+entry.getFilename, remote+"/"+entry.getFilename)
              //log.info(s"Arquivo copiado com sucesso ao diretorio remoto: ${entry.getFilename}")
            } else if(entry.getAttrs.isDir()){
              putLocalFile(sftp, local+"/"+entry.getFilename, remote)
            }
          }
        )
      } else {
        sftp.put(local.toString(), remote+"/"+local.getName)
        //log.info(s"Arquivo copiado com sucesso ao diretorio remoto: ${local.getName}")
      }
    } catch {
      case e: Throwable => e.getMessage
      //log.error("Erro ao descargar:"+e.getMessage)
    }
  }
  
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
      sftp = (jschSession.openChannel("sftp")).asInstanceOf[ChannelSftp]
      sftp.connect()
      //log.info("Conexão ao servidor SFTP com sucesso")
    } catch{
      case e: Throwable => e.getMessage 
      //log.error(("Erro ao tentar conectar com o servidor SFTP"))
    }
    return sftp
  }
  
  def disconnect(sftp: ChannelSftp){
    if(sftp.isConnected()) {
      sftp.disconnect()
      sftp.exit()
      //log.info(s"Desconexao do servidor SFTP com sucesso")
    }
  }
}
