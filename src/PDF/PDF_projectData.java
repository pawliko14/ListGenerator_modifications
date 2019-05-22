package PDF;

public class PDF_projectData  implements Comparable <PDF_projectData>{
	public String projectGroup;
	public String projectNumber;
	public String projectDate;
	public String projectName;
	public String client;
	
	public PDF_projectData(String a, String b, String n, String c, String d){
		projectGroup = a;
		projectNumber = b;
		projectName = n;
		projectDate="";
		if(!c.equals("") && c.length()>=10)
			projectDate = c.substring(0, 10);
		client = d;
	}

	public String getProjectGroup() {
		return projectGroup;
	}

	public String getProjectNumber() {
		return projectNumber;
	}

	public String getProjectDate() {
		return projectDate;
	}
	
	public String getProjectName(){
		return projectName;
	}
	
	public String getClient(){
		return client;
	}
	
	public String getAllProjectNumber(){
		return (projectGroup+"/"+projectNumber);
	}

	@Override
	public int compareTo(PDF_projectData o) {
		// TODO Auto-generated method stub
		int porownaneDaty = projectDate.compareTo(o.projectDate);
		 
        if(porownaneDaty == 0) {
            return getAllProjectNumber().compareTo(o.getAllProjectNumber());
        }
        else {
            return porownaneDaty;
        }
	}
}
