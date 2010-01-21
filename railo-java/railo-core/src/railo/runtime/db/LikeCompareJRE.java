package railo.runtime.db;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import railo.commons.lang.StringUtil;
import railo.runtime.exp.DatabaseException;
import railo.runtime.exp.PageException;
import railo.runtime.op.Caster;

/**
 * Wildcard Filter
 */
class LikeCompareJRE  {
    
    private static final String specials="{}[]().?+\\^$";
    private static Map patterns=new WeakHashMap();
    
    
    private static Pattern createPattern(SQL sql, String wildcard, String escape) throws PageException {
    	Pattern pattern=(Pattern) patterns.get(wildcard+escape);
        if(pattern!=null) return pattern;
        char esc=0;
        if(!StringUtil.isEmpty(escape)){
        	esc=escape.charAt(0);
        	if(escape.length()>1)throw new DatabaseException("Invalid escape character ["+escape+"] has been specified in a LIKE conditional",null,null,sql,null);
        }
        
    	StringBuffer sb = new StringBuffer(wildcard.length());
        int len=wildcard.length();
        //boolean isEscape=false;
        char c;
        for(int i=0;i<len;i++) {
            c = wildcard.charAt(i);
            if(c == esc){
            	if(i+1==len)throw new DatabaseException("Invalid Escape Sequence. Valid sequence pairs for this escape character are: ["+esc+"%] or ["+esc+"_]",null,null,sql,null);
            	c = wildcard.charAt(++i);
            	if(c == '%')sb.append(c);
            	else if(c == '_') sb.append(c);
            	else throw new DatabaseException("Invalid Escape Sequence ["+esc+""+c+"]. Valid sequence pairs for this escape character are: ["+esc+"%] or ["+esc+"_]",null,null,sql,null);
            }
            else {
            	if(c == '%')sb.append(".*");
                else if(c == '_') sb.append('.');
                else if(specials.indexOf(c)!=-1)sb.append('\\').append(c);
                else sb.append(c);
            }
            
        }    
        try {
        	patterns.put(wildcard+escape,pattern=Pattern.compile(sb.toString(),Pattern.DOTALL));
		} 
        catch (PatternSyntaxException e) {
        	throw Caster.toPageException(e);
        }
        return pattern;
    }
    
    public static boolean like(SQL sql, String haystack, String needle) throws PageException {
    	return like(sql, haystack, needle, null);
    }
    
    public static boolean like(SQL sql, String haystack, String needle,String escape) throws PageException {
    	haystack=StringUtil.toLowerCase(haystack);
    	Pattern p = createPattern(sql,StringUtil.toLowerCase(needle),escape==null?null:StringUtil.toLowerCase(escape));
    	return p.matcher(haystack).matches();
    }
    	
}