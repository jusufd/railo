<cfsavecontent variable="c">R0lGODlhBAAKAIABAGZmZv///yH5BAEAAAEALAAAAAAEAAoAAAILjI8GBxu9oHzOhQIAOw==</cfsavecontent><cfoutput><cfif getBaseTemplatePath() EQ getCurrentTemplatePath()><cfcontent type="image/gif" variable="#toBinary(c)#"><cfsetting showdebugoutput="no"><cfelse>data:image/gif;base64,#c#</cfif></cfoutput>