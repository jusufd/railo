package railo.runtime.tag;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import railo.commons.db.DBUtil;
import railo.commons.lang.StringUtil;
import railo.runtime.PageContext;
import railo.runtime.db.DataSourceManager;
import railo.runtime.db.DatasourceConnection;
import railo.runtime.db.SQL;
import railo.runtime.db.SQLImpl;
import railo.runtime.db.SQLItem;
import railo.runtime.db.SQLItemImpl;
import railo.runtime.debug.DebuggerImpl;
import railo.runtime.exp.ApplicationException;
import railo.runtime.exp.PageException;
import railo.runtime.ext.tag.TagImpl;
import railo.runtime.listener.ApplicationContextPro;
import railo.runtime.type.List;
import railo.runtime.type.QueryImpl;
import railo.runtime.type.Struct;
import railo.runtime.type.StructImpl;
import railo.runtime.type.scope.Form;

/**
* Inserts records in data sources.
*
*
*
**/
public final class Insert extends TagImpl {

	/** If specified, password overrides the password value specified in the ODBC setup. */
	private String password;

	/** Name of the data source that contains your table. */
	private String datasource;

	/** If specified, username overrides the username value specified in the ODBC setup. */
	private String username;

	/** A comma-separated list of form fields to insert. If this attribute is not specified, all 
	** 	fields in the form are included in the operation. */
	private String formfields;

	/** For data sources that support table ownership such as SQL Server, Oracle, and Sybase SQL 
	** 	Anywhere, use this field to specify the owner of the table. */
	private String tableowner="";

	/** Name of the table you want the form fields inserted in. */
	private String tablename;

	/** For data sources that support table qualifiers, use this field to specify the qualifier for the 
	** 	table. The purpose of table qualifiers varies across drivers. For SQL Server and Oracle, the qualifier 
	** 	refers to the name of the database that contains the table. For the Intersolv dBase driver, the 
	** 	qualifier refers to the directory where the DBF files are located. */
	private String tablequalifier="";

	/**
	* @see javax.servlet.jsp.tagext.Tag#release()
	*/
	public void release()	{
		super.release();
		password=null;
		username=null;
		formfields=null;
		tableowner="";
		tablequalifier="";
		datasource=null;
	}

	/** set the value password
	*  If specified, password overrides the password value specified in the ODBC setup.
	* @param password value to set
	**/
	public void setPassword(String password)	{
		this.password=password;
	}

	/** set the value datasource
	*  Name of the data source that contains your table.
	* @param datasource value to set
	**/
	public void setDatasource(String datasource)	{
		this.datasource=datasource;
	}

	/** set the value username
	*  If specified, username overrides the username value specified in the ODBC setup.
	* @param username value to set
	**/
	public void setUsername(String username)	{
		this.username=username;
	}

	/** set the value formfields
	*  A comma-separated list of form fields to insert. If this attribute is not specified, all 
	* 	fields in the form are included in the operation.
	* @param formfields value to set
	**/
	public void setFormfields(String formfields)	{
		this.formfields=formfields.toLowerCase().trim();
	}

	/** set the value tableowner
	*  For data sources that support table ownership such as SQL Server, Oracle, and Sybase SQL 
	* 	Anywhere, use this field to specify the owner of the table.
	* @param tableowner value to set
	**/
	public void setTableowner(String tableowner)	{
		this.tableowner=tableowner;
	}

	/** set the value tablename
	*  Name of the table you want the form fields inserted in.
	* @param tablename value to set
	**/
	public void setTablename(String tablename)	{
		this.tablename=tablename;
	}

	/** set the value tablequalifier
	*  For data sources that support table qualifiers, use this field to specify the qualifier for the 
	* 	table. The purpose of table qualifiers varies across drivers. For SQL Server and Oracle, the qualifier 
	* 	refers to the name of the database that contains the table. For the Intersolv dBase driver, the 
	* 	qualifier refers to the directory where the DBF files are located.
	* @param tablequalifier value to set
	**/
	public void setTablequalifier(String tablequalifier)	{
		this.tablequalifier=tablequalifier;
	}


	/**
	* @see javax.servlet.jsp.tagext.Tag#doStartTag()
	*/
	public int doStartTag()	{
		return SKIP_BODY;
	}

	/**
	 * @throws PageException
	 * @see javax.servlet.jsp.tagext.Tag#doEndTag()
	*/
	public int doEndTag() throws PageException	{
		datasource=getDatasource(pageContext,datasource);
		
		
		
		DataSourceManager manager = pageContext.getDataSourceManager();
	    DatasourceConnection dc=manager.getConnection(pageContext,datasource,username,password);
	    try {
	    	
	    	Struct meta =null;
	    	try {
	    		meta=getMeta(dc,tablequalifier,tableowner,tablename);
	    	}
	    	catch(SQLException se){
	    		meta=new StructImpl();
	    	}
		    
	    	
	    	SQL sql=createSQL(meta);
			if(sql!=null) {
				railo.runtime.type.Query query = new QueryImpl(dc,sql,-1,-1,-1,"query");
				
				if(pageContext.getConfig().debug()) {
					boolean debugUsage=DebuggerImpl.debugQueryUsage(pageContext,query);
					((DebuggerImpl)pageContext.getDebugger()).addQuery(debugUsage?query:null,datasource,"",sql,query.getRecordcount(),pageContext.getCurrentPageSource(),query.executionTime());
				}
			}
			return EVAL_PAGE;
	    } 
	    finally {
	    	manager.releaseConnection(pageContext,dc);
	    }
	}

	
	

	public static String getDatasource(PageContext pageContext, String datasource) throws ApplicationException {
		if(StringUtil.isEmpty(datasource)){
			datasource=((ApplicationContextPro)pageContext.getApplicationContext()).getDefaultDataSource();

			if(StringUtil.isEmpty(datasource))
				throw new ApplicationException(
						"attribute [datasource] is required, when no default datasource is defined",
						"you can define a default datasource as attribute [defaultdatasource] of the tag cfapplication or as data member of the application.cfc (this.defaultdatasource=\"mydatasource\";)");
		}
		return datasource;
	}

	public static Struct getMeta(DatasourceConnection dc,String tableQualifier, String tableOwner, String tableName) throws SQLException {
    	DatabaseMetaData md = dc.getConnection().getMetaData();
    	Struct  sct=new StructImpl();
		ResultSet columns = md.getColumns(tableQualifier, tableOwner, tableName, null);
		
		try{
			String name;
			while(columns.next()) {
				name=columns.getString("COLUMN_NAME");
				sct.setEL(name, new ColumnInfo(name,columns.getInt("DATA_TYPE"),columns.getBoolean("IS_NULLABLE")));
				
			}
		}
		finally {
			DBUtil.closeEL(columns);
		}
		return sct;
	}

	/**
     * @param meta 
	 * @return return SQL String for insert
     * @throws PageException
     */
    private SQL createSQL(Struct meta) throws PageException {
        String[] fields=null; 
        Form form = pageContext.formScope();
        if(formfields!=null) fields=List.toStringArray(List.listToArrayRemoveEmpty(formfields,','));
        else fields=pageContext.formScope().keysAsString();
        
        StringBuffer names=new StringBuffer();
        StringBuffer values=new StringBuffer();
        ArrayList items=new ArrayList();
        String field;
        for(int i=0;i<fields.length;i++) {
            field = StringUtil.trim(fields[i],null);
            if(StringUtil.startsWithIgnoreCase(field, "form."))
            	field=field.substring(5);
            
            if(!field.equalsIgnoreCase("fieldnames")) {
                if(names.length()>0) {
                    names.append(',');
                    values.append(',');
                }
                names.append(field);
                values.append('?');
                ColumnInfo ci=(ColumnInfo) meta.get(field,null);
                if(ci!=null)items.add(new SQLItemImpl(form.get(field,null),ci.getType())); 
                else items.add(new SQLItemImpl(form.get(field,null))); 
            }
        }
        if(items.size()==0) return null;
        
        StringBuffer sql=new StringBuffer();
        sql.append("insert into ");
        if(tablequalifier.length()>0) {
            sql.append(tablequalifier);
            sql.append('.');
        }
        if(tableowner.length()>0) {
            sql.append(tableowner);
            sql.append('.');
        }
        sql.append(tablename);
        sql.append('(');
        sql.append(names);
        sql.append(")values(");
        sql.append(values);
        sql.append(")");
        
        return new SQLImpl(sql.toString(),(SQLItem[])items.toArray(new SQLItem[items.size()]));
    }
    
    
    
    
    
    
    
    
    
}

class    ColumnInfo {

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the nullable
	 */
	public boolean isNullable() {
		return nullable;
	}

	private String name;
	private int type;
	private boolean nullable;

	public ColumnInfo(String name, int type, boolean nullable) {
		this.name=name;
		this.type=type;
		this.nullable=nullable;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return name+"-"+type+"-"+nullable;
	}
	
}