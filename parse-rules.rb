require 'CSV'

# column order should be - Language,Category,SubCategory,Tag,Priority,Abstract,Explanation,References
# first row will be dropped
COLS = 7;
LANG = 0;
CAT = 1;
SUBCAT = 2;
KEY = 3;
PRIORITY = 4;
ABSTRACT = 5;
EXPLANATION = 6;
REFS = 7;

file = 'rules.csv'
language_mapping = { 'dotnet' => 'cs', 'python' => 'py', 'javascript' => 'js', 'jsp' => 'web', 'tsql' => 'sql', 'actionscript' => 'flex'}
copy_langs = { 'java' => 'web', 'config' => 'xml'}
global_langs = ['config', 'content']
supported_langs = ['abap', 'cobol', 'cpp', 'cs', 'java', 'js', 'sql', 'vb', 'web', 'py', 'xml', 'flex', 'php', 'objc']

def duplicateRules(rules, origLang, newLang)
  rules.select { |rule|
	rule[LANG].eql?(origLang)
  }.map { |rule|
	newRule = Array.new(rule)
	newRule[LANG] = newLang
	newRule
  }.each do |newRule|
	rules.push(newRule)
  end
end

def getRuleName(rule)
  return rule[CAT] + (rule[SUBCAT].nil? ? '' : ': ' + rule[SUBCAT])
end

def getRuleXml(rule)
  name = getRuleName(rule)
  return "\t<rule>\n\t\t<key>#{rule[KEY]}</key><priority>#{rule[PRIORITY]}</priority><configKey>#{name}</configKey>\n\t</rule>\n"
end

def htmlify(str)
  return str.nil? ? '' : str.gsub("\n", "<br />")
end

def getRuleHtml(rule)
  return "<h2>ABSTRACT</h2>\n<p>\n  " + htmlify(rule[ABSTRACT]) +
    "\n</p>\n<h2>EXPLANATION</h2>\n<p>\n  " + htmlify(rule[EXPLANATION]) + 
	"\n</p>\n<h2>REFERENCES</h2>\n<p>\n  " + htmlify(rule[REFS]) +
	"\n</p>"
end

rules = CSV.read(file)
rules = rules.drop(1)

copy_langs.each { |from, to|
  duplicateRules(rules, from, to)
}

# get uniq set of languages
languages = rules.map { |rule| rule[LANG] }.uniq.sort

languages.each do |lang|
  global_langs.each do |gl|
    if !gl.eql?(lang)
      duplicateRules(rules, gl, lang)
	end
  end
end

# remove global rules
rules = rules.keep_if { |rule| global_langs.index(rule[LANG]).nil? }
languages.keep_if { |lang| global_langs.index(lang).nil? }
# sort by lang
rules = rules.sort { |a,b|
  a[LANG] <=> b[LANG]
}.uniq { |rule|
  rule[LANG] + "#" + rule[CAT] + "#" + (rule[SUBCAT]||'')
}

# build xml files for each language
languages.each do |language|
  sonar_language=language_mapping[language]||language
  if supported_langs.index(sonar_language).nil?
    next
  end
  open("src/main/resources/org/sonar/plugins/fortify/base/rules-#{sonar_language}.xml", "w+") do |xmlFile|
    xmlFile.write("<rules>\n\t<!-- see names and descriptions in org/sonar/l10n/ -->\n")
    rules.each do |rule|
      if rule[LANG].eql?(language)
	    # write rule to xml
        xmlFile.write(getRuleXml(rule))
        
		# create html with description
        open("src/main/resources/org/sonar/l10n/fortify/rules/fortify-#{sonar_language}/#{rule[KEY]}.html", "w+") do |htmlFile|
          htmlFile.write(getRuleHtml(rule))
        end
		
		# for properties file
		puts "rule.fortify-#{sonar_language}.#{rule[KEY]}.name=#{getRuleName(rule)}"
      end
    end
    xmlFile.write('</rules>')
  end
end