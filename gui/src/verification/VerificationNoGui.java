package verification;

import java.io.File;
import java.util.ArrayList;
import verification.platu.main.Options;
import verification.platu.project.Project;

import lpn.parser.LhpnFile;



/**
 * This class provides a way to run depth-first search and partial order reduction (in platu package)
 * without the need for a GUI.
 * @author Zhen Zhang
 *
 */

public class VerificationNoGui {
	
	public static void main (String[] args) {
		if (args.length == 0) {
			System.err.println("Error: Missing arguments.");
			System.exit(0);
		}
		//System.out.println("Enter the directory of all LPNs: ");
		//Scanner scanner = new Scanner(System.in);
		//String directory = scanner.nextLine();
		String directory = null; 
		boolean poroff = true;
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-portb")) {
				Options.setPOR("tb");
				Options.setCycleClosingMthd("behavioral");
				Options.setCycleClosingAmpleMethd("cctb");
				poroff = false;
			}
			else if (args[i].equals("-portboff")) {
				Options.setPOR("tboff");
				Options.setCycleClosingMthd("behavioral");
				Options.setCycleClosingAmpleMethd("cctboff");
				poroff = false;
			}
			else if (args[i].contains("-dir=")) { // Directory should be provided as an argument starting with -dir:
				directory = args[i].trim().substring(5);
			}
			else if (args[i].contains("-log=")) { // Runtime, memory usage, and state count etc are written in the log file specified here.
				Options.setLogName(args[i].trim().substring(5));
			}
			else if (args[i].contains("-memlim=")) {
				Options.setMemoryUpperBoundFlag();
				String memUpperBound = args[i].trim().replace("-memlim=", "");
				if(memUpperBound.contains("G")) {
					memUpperBound = memUpperBound.replace("G", "");					
					Options.setMemoryUpperBound((long)(Float.parseFloat(memUpperBound) * 1000000000));
				}
				if(memUpperBound.contains("M")) {
					memUpperBound = memUpperBound.replace("M", "");
					Options.setMemoryUpperBound((long)(Float.parseFloat(memUpperBound) * 1000000));
				}
			}
			else if (args[i].contains("-disableDeadlock")) {
				Options.disablePORdeadlockPreserve();
			}
		}
		if (directory.trim().equals("") || directory == null) {
			System.out.println("Direcotry provided is empty. Exit.");
			System.exit(0);
		}
		File dir = new File(directory);
		if (!dir.exists()) {
			System.err.println("Invalid direcotry. Exit.");
			System.exit(0);
		}
		Options.setPrjSgPath(directory);
		// Options for printing the final numbers from search_dfs or search_dfsPOR. 
		Options.setOutputLogFlag(true);
		Options.setDebugMode(false);
				
		File[] lpns = dir.listFiles(new FileExtentionFilter(".lpn"));
		ArrayList<LhpnFile> lpnList = new ArrayList<LhpnFile>();
		for (int i=0; i < lpns.length; i++) {
		 String curLPNname = lpns[i].getName();
		 LhpnFile curLPN = new LhpnFile();
		 curLPN.load(directory + curLPNname);
		 lpnList.add(curLPN);
		}		
		
		Project untimed_dfs = new Project(lpnList);
		if (poroff)
			untimed_dfs.search();
		else 
			untimed_dfs.searchPOR();
		
	}
}