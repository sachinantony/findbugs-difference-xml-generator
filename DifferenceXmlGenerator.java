package com.ri.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DifferenceXmlGenerator {

	public static void main(String[] args) {
		try
		{
			List<String> hashList= new ArrayList<String>();
			List<String> hashList2= new ArrayList<String>();
			List<String> classList= new ArrayList<String>();
			List<String> packageList= new ArrayList<String>();
			List<String> fileList = new ArrayList<String>();
			List<String> priority1List= new ArrayList<String>();
			List<String> priority2List= new ArrayList<String>();			
			List<Node> toRemoveList=new ArrayList<Node>();
			Set<String> packageSet= new HashSet<String>();
			Set<String> classSet= new HashSet<String>();	
			
			//The first and second findbugs reports
			File file2=new File(args[1]);
			File file1=new File(args[0]);
			DocumentBuilderFactory dbFactory =DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder=dbFactory.newDocumentBuilder();
			//checking whether both the files exist
			if(file1.exists()&&file2.exists())
			{
				Document doc1=dBuilder.parse(file1);
				Document doc2=dBuilder.parse(file2);
				Node root=doc2.getFirstChild();
				doc1.getDocumentElement().normalize();
				doc2.getDocumentElement().normalize();
				//Getting all nodes with tag name "BugInstance" in both files
				NodeList bugInstanceList1=doc1.getElementsByTagName("BugInstance");
				NodeList bugInstanceList2=doc2.getElementsByTagName("BugInstance");
				//Adding all the instance hashes in file1 to an arraylist
				for(int i=0;i<bugInstanceList1.getLength();i++)
				{
					Node nNode=bugInstanceList1.item(i);
					Element eElement=(Element)nNode;
					String instanceHash=eElement.getAttribute("instanceHash");
					hashList.add(instanceHash);
				}
				//Adding all instance hashes in file2 to an arraylist
				for(int i=0;i<bugInstanceList2.getLength();i++)
				{
					Node nNode=bugInstanceList2.item(i);
					Element eElement=(Element)nNode;
					String instanceHash=eElement.getAttribute("instanceHash");
					hashList2.add(instanceHash);
				}
				// getting instance hashes unique to the new report
				hashList2.removeAll(hashList);
				for(int i=0;i<bugInstanceList2.getLength();i++)
				{
					Node nNode=bugInstanceList2.item(i);
					Element eElement=(Element)nNode;
					String instanceHash=eElement.getAttribute("instanceHash");
					if(!hashList2.contains(instanceHash))
					{	
						//removing the BugInstance nodes present in first report
						toRemoveList.add(nNode);
					}
					else
					{
						NodeList childNodes=nNode.getChildNodes();
						for(int j=0;j<childNodes.getLength();j++)
						{
							Node cNode=childNodes.item(j);
							//getting child nodes of name "Class" and getting arraylists of packagename,classname and filename
							if(cNode.getNodeName().equals("Class"))
							{
								Element cElement=(Element)cNode;
								String className=cElement.getAttribute("classname");
								classList.add(className);
								String fileName=className.replace(".","/")+".java";
								fileList.add(fileName);
								String[] temp=className.split("\\.");
								temp[temp.length-1]="";	
								StringBuilder sb=new StringBuilder("");
								for(int k=0;k<temp.length;k++)
								{
									if(k>0&&k<temp.length-1)
										sb.append(".");
									sb.append(temp[k]);
								}						
								String packageName=sb.toString();
								packageList.add(packageName);
								if(eElement.getAttribute("priority").equals("1"))
								{
									priority1List.add(packageName);
								}
								else if(eElement.getAttribute("priority").equals("2"))
								{
									priority2List.add(packageName);
								}
								packageSet.addAll(packageList);
								classSet.addAll(classList);
							}
						}
					}
				}
				for(Node n1:toRemoveList)
				{
					root.removeChild(n1);
				}
				NodeList summaryList=doc2.getElementsByTagName("FindBugsSummary");
				Node summary=summaryList.item(0);
				NamedNodeMap attr1=summary.getAttributes();
				Node total_bugsAttr1=attr1.getNamedItem("total_bugs");
				int totalBugsNo1=hashList2.size();
				String total_bugs1=String.valueOf(totalBugsNo1);
				total_bugsAttr1.setTextContent(total_bugs1);
				
				Node total_classesAttr=attr1.getNamedItem("total_classes");
				int total_classesNo=classSet.size();
				String total_classes=String.valueOf(total_classesNo);
				total_classesAttr.setTextContent(total_classes);
				
				Node num_packagesAttr=attr1.getNamedItem("num_packages");
				int num_packagesNo=packageSet.size();
				String num_packages=String.valueOf(num_packagesNo);
				num_packagesAttr.setTextContent(num_packages);
				
				Node priority1Attr1=attr1.getNamedItem("priority_1");
				int priority1No1=priority1List.size();
				String priority_1_1=String.valueOf(priority1No1);
				priority1Attr1.setTextContent(priority_1_1);
				
				Node priority2Attr1=attr1.getNamedItem("priority_2");
				int priority2No1=priority2List.size();
				String priority_2_1=String.valueOf(priority2No1);
				priority2Attr1.setTextContent(priority_2_1);
				
				NodeList sumChildList=summary.getChildNodes();
				for(int i=0;i<sumChildList.getLength();i++)
				{
					Node sumChild=sumChildList.item(i);
					if(sumChild.getNodeType()==Node.ELEMENT_NODE)
					{
						Element sumElement=(Element)sumChild;
						if(sumElement.getNodeName().equals("FileStats"))
						{
							int bugCountNo=Collections.frequency(fileList, sumElement.getAttribute("path"));
							String bugCount=String.valueOf(bugCountNo);
							NamedNodeMap attr = sumChild.getAttributes();
							Node bugCountAttr=attr.getNamedItem("bugCount");
							bugCountAttr.setTextContent(bugCount);
						}
						
						else if(sumElement.getNodeName().equals("PackageStats"))
						{
							int totalBugsNo=Collections.frequency(packageList, sumElement.getAttribute("package"));
							int priority1No=Collections.frequency(priority1List, sumElement.getAttribute("package"));
							int priority2No=Collections.frequency(priority2List, sumElement.getAttribute("package"));
							String total_bugs=String.valueOf(totalBugsNo);
							String priority_1=String.valueOf(priority1No);
							String priority_2=String.valueOf(priority2No);
							NamedNodeMap attr=sumChild.getAttributes();
							Node total_bugsAttr=attr.getNamedItem("total_bugs");
							if(sumElement.hasAttribute("priority_1"))
							{
								Node priority_1Attr=attr.getNamedItem("priority_1");
								priority_1Attr.setTextContent(priority_1);					
							}
							if(sumElement.hasAttribute("priority_2"))
							{
								Node priority_2Attr=attr.getNamedItem("priority_2");
								priority_2Attr.setTextContent(priority_2);					
							}						
							total_bugsAttr.setTextContent(total_bugs);
							NodeList packChildList=sumChild.getChildNodes();
							for(int j=0;j<packChildList.getLength();j++)
							{
								Node classNode=packChildList.item(j);
								if(classNode.getNodeType()==Node.ELEMENT_NODE)
								{
									Element classElement=(Element)classNode;
									if(classElement.getNodeName().equals("ClassStats"))
									{
										int bugsNo=Collections.frequency(classList, classElement.getAttribute("class"));
										String bugs=String.valueOf(bugsNo);
										NamedNodeMap attr2=classNode.getAttributes();
										Node bugsAttr=attr2.getNamedItem("bugs");
										bugsAttr.setTextContent(bugs);
										
									}
								}
							}
						}
					}
					
					
				}
						
				TransformerFactory tFactory = TransformerFactory.newInstance();
				Transformer transformer=tFactory.newTransformer();
				DOMSource source=new DOMSource(doc2);
				StreamResult result=new StreamResult(new File(args[2]));
				transformer.transform(source, result);
			}
			else
			{
				System.out.println("The specified file does not exist");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	
		
	}
}

