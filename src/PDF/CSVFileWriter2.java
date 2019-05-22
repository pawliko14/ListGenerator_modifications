package PDF;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class CSVFileWriter2 {
	//Delimiter used in CSV file
	private static String DELIMITER = ";";
	private static final String NEW_LINE_SEPARATOR = "\n";
	
	static FileWriter fileWriter = null;
	
	public CSVFileWriter2(String tytul, String delimiter){
		DELIMITER = delimiter;
		SimpleDateFormat doNazwy = new SimpleDateFormat("yyyy.MM.dd");
		SimpleDateFormat godzina = new SimpleDateFormat("HHmm");
		Calendar date = Calendar.getInstance();
		
		try {
			File theDir = new File(Parameters.getPathToSaveHours()+"/"+doNazwy.format(date.getTime()));
			// if the directory does not exist, create it
			if (!theDir.exists()) {
			    try{
			        theDir.mkdir();
			    } 
			    catch(SecurityException se){
			        //handle it
			    }
			}
			fileWriter = new FileWriter(Parameters.getPathToSave()+"/"+doNazwy.format(date.getTime())+"/"+godzina.format(date.getTime())+tytul+".csv");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void addLine(String[] linia) throws IOException{
		for(int i = 0; i<linia.length; i++){
			fileWriter.append(linia[i]);
			fileWriter.append(DELIMITER);
		}
		fileWriter.append(NEW_LINE_SEPARATOR);
	}
		
			
	public static void close(){
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
			}
			
		}
	
	
}
