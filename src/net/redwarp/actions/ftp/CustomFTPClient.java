package net.redwarp.actions.ftp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ResourceBundle;

import net.redwarp.actions.tools.Tools;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import com.jbbres.lib.actions.elements.ActionExecutionException;

public class CustomFTPClient extends FTPClient {
	private String host;
	private String login;
	private String password;
	private boolean overwrite = false;
	private boolean passive = true;
	private static ResourceBundle bundle = ResourceBundle.getBundle("net.redwarp.actions.locale.CustomFTPClientLocale");
	
	public CustomFTPClient(String host, String login, String password){
		this.host = host;
		this.login = login;
		this.password = password;
	}

	public void connect() throws SocketException, IOException{
		connect(host);
	}
	
	public boolean login() throws IOException {
		return login(login, password);
	}
	
	public void setPassiveMode(boolean passive){
		this.passive = passive;
	}
	
	public void setOverwrite(boolean overwrite){
		this.overwrite = overwrite;
	}
	
	public boolean sendFile(File file, FTPFile[] filesInDirectory) throws ActionExecutionException {
		if(filesInDirectory == null){
			try{
			filesInDirectory = listFiles();
			} catch (IOException e){
				throw new ActionExecutionException(bundle.getString("errorFolderList") + file.getName());
			}
		}

		boolean status = false;
		if(file.isDirectory()){
			if(!isFilenameInFTPDirectory(file.getName(), filesInDirectory)){
				try{
				makeDirectory(file.getName());
				} catch (IOException e){
					throw new ActionExecutionException(bundle.getString("errorFolderCreation") + file.getName());
				}
			}
			try {
				changeWorkingDirectory(file.getName());
				FTPFile[] currentFileList = listFiles();
				File[] childrenFiles = Tools.sortByIsDirectory(file.listFiles());
				for(File childrenFile : childrenFiles){
					sendFile(childrenFile, currentFileList);
				}				
				changeToParentDirectory();
			} catch (IOException e) {
				throw new ActionExecutionException(bundle.getString("errorFolderAccess") + file.getName());
			}
			return true;
		} else {
			/*
			 * Already exist
			 */
			if(isFilenameInFTPDirectory(file.getName(), filesInDirectory)){
				/*
				 * No need to write over, so our work is pretty much done.
				 */
				if(!overwrite){
					return true;
				} else {
					/*
					 * Before rewrite, we delete remote file
					 */
					try{
						status = deleteFile(file.getName());
					} catch (IOException e){
						throw new ActionExecutionException(bundle.getString("errorFileDelete") + file.getName());
					}
					if(status == false){
						return false;
					}
				}
			}
			
			InputStream is;
			try{
				is = new BufferedInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e){
				throw new ActionExecutionException(bundle.getString("errorFileNotFound") + file.getName());
			}
			
			try{
				if(passive){
					enterLocalPassiveMode();
				}
				
				OutputStream os = storeFileStream(file.getName());
				byte[] buffer = new byte[1024];
				int count = 0;
				try{
					while((count = is.read(buffer)) > 0){
						os.write(buffer, 0, count);
					}
				} catch (IOException e){
					return false;
				} finally {
					os.close();
					completePendingCommand();
				}
				status = true;
			} catch (IOException e){
				status = false;
			}
		}
		
		return status;
	}
	
	private boolean isFilenameInFTPDirectory(String filename, FTPFile[] filesInDirectory){
		for(FTPFile file : filesInDirectory){
			if(filename.equals(file.getName())){
				return true;
			}
		}
		return false;
	}
	

	public boolean makeDirectoryRecursive(String pathname) throws IOException {
		boolean status = false;
		int slashAt = 0;
		while((slashAt = pathname.indexOf('/', slashAt + 1)) != -1){
			String subfolder = pathname.substring(0, slashAt);
			System.out.println(subfolder);
			status |= makeDirectory(subfolder);
		}
		System.out.println(pathname);
		status |= makeDirectory(pathname);
		return status;		
	}
}