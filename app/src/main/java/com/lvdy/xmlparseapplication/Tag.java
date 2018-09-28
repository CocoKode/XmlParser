package com.lvdy.xmlparseapplication;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author lvdy
 */

public class Tag {
	
	public static final String CONFIG_INPUT_PATH = Environment.getExternalStorageDirectory() + "/defaultConfig.xml";
	public static final String CONFIG_OUTPUT_PATH = Environment.getExternalStorageDirectory() + "/myConfig.xml";
	private static final String DEFAULT_XML_NAME = "defaultConfig.xml";
	
	public static class Attribute {
		String name;
		String value;
	}
	
	List<Tag> tagList;
	List<Attribute> attributeList;
	String name;
	String value;
	
	void addAttributes(Attribute a) {
		if (attributeList == null) {
			attributeList = new ArrayList<Attribute>();
		}
		attributeList.add(a);
	}
	
	void addTags(Tag t) {
		if (tagList == null) {
			tagList = new ArrayList<Tag>(); 
		}
		tagList.add(t);
	}
	
	Tag getTag(String tagName) {
		for (Tag t : tagList) {
			if (t.name.equals(tagName)) {
				return t;
			}
		}
		return null;
	}
	
	Attribute getAttribute(String attrName) {
		for (Attribute a : attributeList) {
			if (a.name.equals(attrName)) {
				return a;
			}
		}
		return null;
	}
	
	int getTagCount() {
		return tagList == null ? 0 : tagList.size();
	}
	
	int getAttributeCount() {
		return attributeList == null ? 0 : attributeList.size();
	}
	
	public static Tag loadFromPath(String path) {
		try {
			InputStream in = getInputStream(path);
			if (in != null) {
				return loadFromStream(in);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Tag loadFromString(String xmlStr) {
		
		try {
			InputStream in = new ByteArrayInputStream(xmlStr.getBytes("UTF-8"));
			return loadFromStream(in);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static Tag getDefaultTag(Context context) {
		try {
			InputStream in = context.getAssets().open(DEFAULT_XML_NAME);
			return loadFromStream(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static Tag loadFromStream(InputStream in) {
		Tag root = null;
	    
		try {
	        XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
	        XmlPullParser pullParser = pullParserFactory.newPullParser();
	
	        pullParser.setInput(in, "UTF-8");
	        int eventType = pullParser.getEventType();
	        Stack<Tag> tagStack = new Stack<>();
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	            switch (eventType) {
	                case XmlPullParser.START_DOCUMENT:
	                	break;
	                case XmlPullParser.START_TAG:
	                	String tagName = pullParser.getName();
	                	Tag tag = new Tag();
	                	tag.name = tagName;
	                	
	                	int count = pullParser.getAttributeCount();
	                	for (int i = 0; i < count; i++) {
	                		String name = pullParser.getAttributeName(i);
	                		Attribute attr = new Attribute();
	                		attr.name = name;
	                		attr.value = pullParser.getAttributeValue(null, name);
	                		tag.addAttributes(attr);
	                	}
	                	if (!tagStack.empty()) {
	                		root = tagStack.peek();
		                	if (root != null) {
		                		root.addTags(tag);
		                	}
	                	}
	                	
	                	tagStack.push(tag);
	                    break;
	                case XmlPullParser.TEXT:
	                	if (!tagStack.empty()) {
	                		root = tagStack.peek();
		                	if (root != null) {
		                		root.value = pullParser.getText();
		                	}
	                	}
	                	break;
	                case XmlPullParser.END_TAG:
	                	root = tagStack.pop();
	                    break;
	                default:
	                    break;
	            }
	            eventType = pullParser.next();
	        }
		} catch (XmlPullParserException | IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (in != null) {
	            try {
	                in.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    
	    return root;
	}
	
	public static synchronized void generateConfigFile(Tag config, String outputPath) {
		FileOutputStream out = null;
	    try {
	    	out = getOutputStream(outputPath);
		    if (config == null || out == null) {
		        return;
		    }
		    
		    XmlSerializer xmlSerializer = XmlPullParserFactory.newInstance().newSerializer();
	        xmlSerializer.setOutput(out, "UTF-8");
	        xmlSerializer.startDocument("UTF-8", true);
	        
		    Stack<Tag> tagStack = new Stack<>();
		    List<Attribute> attributeList;
		    List<Tag> tagList;
			Tag tag;
			
			tagStack.push(config);
			while (!tagStack.empty()) {
				tag = tagStack.pop();
				
				if (tag.name.equals("end_tag")) {
					xmlSerializer.endTag(null, tag.value);
					continue;
				}
				
				xmlSerializer.startTag(null, tag.name);
				Tag endTag = new Tag();
		        endTag.name = "end_tag";
		        endTag.value = tag.name;
		        tagStack.push(endTag);
				
				attributeList = tag.attributeList;
				if (attributeList != null && attributeList.size() > 0) {
					for (Attribute attr : attributeList) {
			        	xmlSerializer.attribute(null, attr.name, attr.value);
			        }
				}
		        
		        tagList = tag.tagList;
		        if (tagList != null && tagList.size() > 0) {
		        	Tag t;
			        for (int i = tagList.size() - 1; i >= 0; i--) {
			        	t = tagList.get(i);
			        	tagStack.push(t);
			        }
		        }
			}
	    	
	        xmlSerializer.endDocument();
	        out.getFD().sync();
	    } catch (XmlPullParserException | IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (out != null) {
	            try {
	                out.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	}
	
	public String convert2String() {
		StringBuilder sb = new StringBuilder();
		Stack<Tag> tagStack = new Stack<>();
		boolean empty;
		Tag tag;
		
		tagStack.push(this);
		while (!tagStack.empty()) {
			tag = tagStack.pop();
			
			if (tag.name.equals("end_tag")) {
				sb.append("</").append(tag.value).append(">");
				continue;
			}
			
			empty = (tag.getTagCount() == 0) && (tag.value == null);
	    	
	    	sb.append("<").append(tag.name).append(" ");
	    	
	    	if (tag.attributeList != null && tag.attributeList.size() > 0) {
	    		for (Attribute attr : tag.attributeList) {
		        	if (attr.value == null) attr.value = "";
		        	sb.append(attr.name).append("=").append("\"").append(attr.value).append("\"").append(" ");
		        }
	    	}
	        
	        if (empty) {
	        	sb.append("/>");
	        } else {
	        	sb.append(">");
		        Tag endTag = new Tag();
		        endTag.name = "end_tag";
		        endTag.value = tag.name;
		        tagStack.push(endTag);
	        	
	        	if (tag.value != null) {
	        		sb.append(tag.value);
	        	}
	        	
	        	if (tag.tagList != null && tag.tagList.size() > 0) {
	        		Tag t;
			        for (int i = tagList.size() - 1; i >= 0; i--) {
			        	t = tagList.get(i);
			        	tagStack.push(t);
			        }
	        	}
	        }
		}
        
        return sb.toString();
	}
	
	public static void modifyTag(Tag srcTag, Tag dstTag) {
		Stack<Tag> srcStack = new Stack<>();
		Stack<Tag> dstStack = new Stack<>();
		Tag st;
		Tag dt;
		List<Attribute> attrs;
		List<Tag> tags;
		Attribute dstAttr;
		Tag dst;
		srcStack.push(srcTag);
		dstStack.push(dstTag);
		while (!srcStack.empty()) {
			st = srcStack.pop();
			dt = dstStack.pop();
			
			attrs = st.attributeList;
			if (attrs != null) {
				for (Attribute attr : attrs) {
					dstAttr = dt.getAttribute(attr.name);
					if (dstAttr != null) {
						dstAttr.value = attr.value;
					}
		        }
			}
			
			tags = st.tagList;
			if (tags != null) {
				for (Tag src : tags) {
					dst = dt.getTag(src.name);
		        	if (dst != null) {
		        		srcStack.push(src);
		        		dstStack.push(dst);
		        	}
		        }
			}
		}
	}
	
    private static FileInputStream getInputStream(String path) throws FileNotFoundException {
		if (!TextUtils.isEmpty(path)) {
			File file = new File(path);
			if (file.exists()) {
				return new FileInputStream(file);
			} else {
				Log.i("Tag", "file does not exist");
				return null;
			}
		}

        return null;
    }

	/**
	 * Return the outputStream of the file with path.If the file doesn't exist,create a new file and return.
	 * @param path
	 * @return
	 * @throws IOException
	 */
	private static FileOutputStream getOutputStream(String path) throws IOException {
		if (!TextUtils.isEmpty(path)) {
			File file = new File(path);
			if (!file.exists()) {
				if (!file.createNewFile()) {
					Log.i("Tag", "fail to create file");
				}
			}
			return new FileOutputStream(file);
		}

        return null;
    }
}
