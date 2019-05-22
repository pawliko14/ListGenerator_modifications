package PDF;

public class Parameters {

//	private static String PathToSave= "//192.168.90.203/Logistyka/Listy";
//	private static String PathToSaveHours= "//192.168.90.203/Logistyka/Raporty godzin";
//	private static String PathToOSN= "//192.168.90.203/OSN/Listy programistow";
//	private static String PathToDokumentacjaHacoSoft= "//192.168.90.203/DokumentacjaHacosoft/Listy programistow";
	
	// do testow - zapisuje lokalnie
	private static String PathToSave= "\\\\192.168.90.203\\common\\Listy_testowe";
	private static String PathToSaveHours= "\\\\192.168.90.203\\common\\Listy_testowe";
	private static String PathToOSN= "\\\\192.168.90.203\\common\\Listy_testowe";
	private static String PathToDokumentacjaHacoSoft= "\\\\192.168.90.203\\common\\Listy_testowe";
	
	
	public static String getPathToSave(){
		return PathToSave;
	}
	
	public static String getPathToSaveHours(){
		return PathToSaveHours;
	}
	
	public static void setPathToSave (String s){
		PathToSave = s;
	}
	
	public static String getPathToOSN() {
		return PathToOSN;
	}

	public static void setPathToOSN(String pathToOSN) {
		PathToOSN = pathToOSN;
	}
	
	public static String getPathToDokumentacjaHacoSoft() {
		return PathToDokumentacjaHacoSoft;
	}

	public static void setPathToDokumentacjaHacoSoft(String pathToDokumentacjaHacoSoft) {
		PathToDokumentacjaHacoSoft = pathToDokumentacjaHacoSoft;
	}
	
}
