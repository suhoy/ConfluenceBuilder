# ConfluenceBuilder
Create page in confluence by template    
Works with conluence 7.19.2 and java 8   

### Arguments
```java
-title      Page title, should be unique in confluence space  
-config     Properties file with token, urls and etc  
-html       Html page template  
-json       Json request template for page creation  
-parent     Parent page id  
-desc       Page description, unnecessary argument, usage depends on html template  
-time       Test time, unnecessary argument, usage depends on html template  
-link       Test case link, unnecessary argument, usage depends on html template  
-build      Build version, unnecessary argument, usage depends on html template  
-name       Output file name (date and time will be added)  
-graphs     Folder with graphs that mentioned  in html template and should be uploaded as attachments  
```

### Start example
```java 
java -jar ConfluenceBuilder-1.0.jar -title "Unique Page Title" -config .\config.properties -html .\body.html -json .\template.json -parent 82678896 -desc "Description text" -time "12:00 - 13:00" -link "https://github.com" -build "1.0.0" -graphs .\graphs  
```  
  
  
### Config.properties  
```properties
confluence.url=https://confluence.local
confluence.token=your_confluence_bearer_token_123
confluence.space=SPACEID
confluence.page.add=/rest/api/content/
confluence.attachment.add=/rest/api/content/${page}/child/attachment
```

### Body.html
```html
<h2>Информация о тесте</h2>
<p><strong>Описание: </strong>${desc}</p>
<p><strong>Билд: </strong>${build}</p>
<p><strong>Ссылка: </strong><a class="external-link" href="${link}" rel="nofollow">${link}</a></p>
<p><strong>Время теста: </strong>${time}</p>
<p><br></br></p>
<!-- table of content macro -->
<ac:structured-macro ac:name="toc">
<ac:parameter ac:name="printable">true</ac:parameter>
<ac:parameter ac:name="style">square</ac:parameter>
<ac:parameter ac:name="maxLevel">2</ac:parameter>
<ac:parameter ac:name="indent">5px</ac:parameter>
<ac:parameter ac:name="minLevel">2</ac:parameter>
<ac:parameter ac:name="class">bigpink</ac:parameter>
<ac:parameter ac:name="exclude">[1//2]</ac:parameter>
<ac:parameter ac:name="type">list</ac:parameter>
<ac:parameter ac:name="outline">true</ac:parameter>
<ac:parameter ac:name="include">.*</ac:parameter>
</ac:structured-macro>
<p><br></br></p>
<h2>Графики</h2>
<p><ac:image><ri:attachment ri:filename="example.png"></ri:attachment></ac:image></p>
<p style="text-align: center;"><strong>Рисунок example.png</strong></p>
<p><br></br></p>
<p><ac:image><ri:attachment ri:filename="example2.png"></ri:attachment></ac:image></p>
<p style="text-align: center;"><strong>Рисунок example2.png</strong></p>
<p><br></br></p>
```

### Template.json
```config
{
    "type": "page",
    "title": "${title}",
    "ancestors": [{
            "id": ${parent}
        }],
    "body": {
        "storage": {
        "value": "${html}",
        "representation": "storage"
        }
    },
    "space": {
        "key": "${space}"
    }
}
```
