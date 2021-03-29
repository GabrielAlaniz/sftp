package com.telefonica

import java.io.File
import org.apache.hadoop.conf.Configuration
import com.jcraft.jsch.JSch
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import org.apache.log4j.Logger

class FileTransfer extends Serializable {
  @transient
  lazy val log: Logger = Logger.getLogger(getClass.getName)
  
  def SftpTransferFromHdfs(hadoopConf: Configuration, hdfsDirectory: String, localDirectory: String,
    user: String, pass: String, hostname: String, targetDirectory: String): Unit = {
      val ssh = new JSch()
      val fileSystem = FileSystem.get(hadoopConf)
      val files = fileSystem.listStatus(new Path(hdfsDirectory))
      val local = new File(localDirectory)
      try {
        val session = ssh.getSession(user, hostname)
        session.setPassword(pass)
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect()
        if (session.isConnected) {
          val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
          channel.connect()
          if (channel.isConnected) {
        	  createRemoteDirectory(channel, targetDirectory)
        	  files.foreach(hdfsFile => {
        		  if (hdfsFile.getPath.getName.endsWith(".csv")) {
        			  log.info(s"Realizando o download do arquivo do HDFS: ${hdfsFile.getPath} para o diretorio local: ${local.getAbsolutePath}")
        			  if (!local.isDirectory) {
        				  local.mkdir()
        			  }
        			  fileSystem.copyToLocalFile(false, hdfsFile.getPath, new Path(localDirectory))
        			  //fileSystem.copyToLocalFile(delSrc, src, dst)

        			  val localFile = new File(localDirectory + "/" + hdfsFile.getPath.getName)
        			  if (localFile.isFile) {
        				  channel.put(localDirectory + "/" + hdfsFile.getPath.getName, targetDirectory)
        				  log.info(s"Removendo o arquivo local: ${localFile.getAbsolutePath}")
        				  localFile.delete()
        			  }
        		  }
        	  })
        	  channel.disconnect()
          }
          session.disconnect()
        } else {
        	throw new Exception(s"Erro ao tentar conectar no SFTP: $user@$hostname")
        }
      } catch {
      case e: Throwable =>
      e.printStackTrace()
      log.error("Erro ao tentar enviar os arquivos para o sftp: " + e.getMessage)
      }
  }

  def clear(hadoopConf: Configuration, hdfs: String, unix: String): Unit = {
		  log.info("Realizando a limpeza dos diretorios criados pelo processo!")
		  FileSystem
		  .get(hadoopConf)
		  .delete(new Path(hdfs), true)
		  
		  new File(unix)
		  .listFiles
		  .foreach(_.delete())
		  
		  new File(unix)
		  .delete()
		  
		  log.info("Diretorios temporários removidos")
  }

  def createRemoteDirectory(channel: ChannelSftp, directory: String): Unit = {
	  val fullPath = directory.split("/")
	  channel.cd("/")
	  for (folder <- fullPath) {
		  if (folder.length > 0) {
			  try {
          log.info("Current Dir : " + channel.pwd)
				  channel.cd(folder)
			  } catch {
			  case e: SftpException =>
			  channel.mkdir(folder)
			  channel.cd(folder)
			  }
		  }
	  }
  }
}
