/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rl.sqltrainer.domain.Exercise;
import rl.sqltrainer.domain.ExerciseSet;
import rl.util.exceptions.EncryptionException;
import rl.util.exceptions.ErrorHandler;
import rl.util.exceptions.PersistenceException;
import rl.util.security.SecureHashService;

/**
 * This class implements transformations between internal object-oriented
 * representations and external XML representations of domain objects for the
 * SQL Trainer.
 * 
 * @author Ruediger Lunde
 */
public class XMLFileHandler {
	/** Local path for accessing DTDs and XSLT-files. */
	private static File DTD_PATH = new File("xml");

	/** Sets the local path for accessing DTDs and XSLT-files. */
	public static void setDtdPath(File path) {
		DTD_PATH = path;
	}

	/** Loads an exercise set from an XML file. */
	public ExerciseSet loadExerciseSet(File file) throws PersistenceException {
		ExerciseSet result = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(true);
			// factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new MyEntityResolver());
			builder.setErrorHandler(new MyErrorHandler());
			Document doc = builder.parse(file);
			boolean containsAnswers = false;
			Node node = doc.getElementsByTagName("exercise-set").item(0);
			String hashA = getAttValue(node, "hasha");

			result = new ExerciseSet(getAttValue(node, "editor"), getAttValue(
					node, "lastEdit"), getAttValue(node, "hashs"), getAttValue(
					node, "passwd"));
			result.setData(getAttValue(node, "course"),
					getAttValue(node, "lecturer"), getAttValue(node, "id"),
					getAttValue(node, "db"));

			node = doc.getElementsByTagName("intro").item(0);
			result.setIntro(getXMLData(node));
			NodeList eNodes = doc.getElementsByTagName("exercise");
			for (int i = 0; i < eNodes.getLength(); i++) {
				Exercise ex = readExercise(eNodes.item(i));
				containsAnswers |= !ex.getAnswer().isEmpty();
				result.addExercise(ex);
			}
			if (containsAnswers && !hashA.equals(computeHashValue(result)))
				result.setEditor("??");
		} catch (Exception e) {
			PersistenceException pe = new PersistenceException(
					"Loading exercise set " + file + " failed.", e);
			throw pe;
		}
		return result;
	}

	/**
	 * Helper method for accessing attributes. If the specified attribute does
	 * not exist, the empty string is returned.
	 */
	private String getAttValue(Node node, String attName) {
		String result = "";
		Node att = node.getAttributes().getNamedItem(attName);
		if (att != null)
			result = att.getNodeValue();
		return result;
	}

	/**
	 * Creates an <code>Exercise</code> instance using the data provided by the
	 * DOM node.
	 */
	private Exercise readExercise(Node eNode) {
		NamedNodeMap atts = eNode.getAttributes();
		String eID = atts.getNamedItem("id").getNodeValue();
		Exercise.Type type = Exercise.Type.OTHER;
		String eTheme = "";
		if (atts.getNamedItem("type") != null
				&& atts.getNamedItem("type").getNodeValue().equals("sql"))
			type = Exercise.Type.SQL;
		if (atts.getNamedItem("theme") != null)
			eTheme = atts.getNamedItem("theme").getNodeValue();
		String eQuest = "";
		String eAnsw = "";
		String eSol = "";
		NodeList children = eNode.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node child = children.item(j);
			String data = "";
			if (child.getFirstChild() instanceof Text)
				data = ((Text) child.getFirstChild()).getData();
			if (child.getNodeName().equals("question")) {
				try {
					eQuest = getXMLData(child);
				} catch (TransformerException te) {
					PersistenceException pe = new PersistenceException(
							"Reading question text of exercise " + eID
									+ " failed.", te);
					ErrorHandler.getInstance().handleError(pe);
					eQuest = data;
				}
			} else if (child.getNodeName().equals("answer")) {
				eAnsw = data;
			} else if (child.getNodeName().equals("solution")) {
				eSol = data;
			}
		}
		Exercise result = new Exercise(eID, type, eTheme, eQuest);
		result.setAnswer(eAnsw);
		result.setSolution(eSol);
		return result;
	}

	/**
	 * Helper method which transforms a hierarchical node structure into a
	 * string with XML tags. It is used to convert HTML substructures into a
	 * String attribute value.
	 * 
	 * @throws TransformerException
	 */
	private String getXMLData(Node node) throws TransformerException {
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		DOMSource source = new DOMSource(node);
		StringWriter writer = new StringWriter();
		StreamResult resultStream = new StreamResult(writer);
		transformer.transform(source, resultStream);
		String result = writer.toString();
		String elName = node.getNodeName();
		int start = result.indexOf("<" + elName + ">");
		int end = result.indexOf("</" + elName + ">");
		return start != -1 ? result.substring(start + elName.length() + 2, end)
				: "";
	}

	/**
	 * Writes an XML representation of a specified exercise set to a specified
	 * file.
	 * 
	 * @throws PersistenceException
	 */
	public void saveExerciseSet(File file, ExerciseSet exercises)
			throws PersistenceException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			// factory.setValidating(true);
			// factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element root = doc.createElement("exercise-set");
			doc.appendChild(root);
			root.setAttribute("course", exercises.getCourse());
			root.setAttribute("db", exercises.getDB());
			root.setAttribute("id", exercises.getID());
			root.setAttribute("lecturer", exercises.getLecturer());
			if (exercises.containsAnswers()) {
				root.setAttribute("editor", exercises.getEditor());
				root.setAttribute("lastEdit", exercises.getLastEdit());
				root.setAttribute("hasha", computeHashValue(exercises));
			}
			if (!exercises.getPasswd().isEmpty())
				root.setAttribute("hashs", exercises.getSolHash());
			root.setAttribute("passwd", exercises.getPasswd());
			StringReader reader = new StringReader("<intro>"
					+ exercises.getIntro() + "</intro>");
			Document tmpDoc = builder.parse(new InputSource(reader));
			Node introNode = doc.importNode(tmpDoc.getFirstChild(), true);
			root.appendChild(introNode);
			for (int i = 0; i < exercises.size(); i++) {
				Exercise exercise = exercises.getExercise(i);
				Element exNode = doc.createElement("exercise");
				root.appendChild(exNode);
				exNode.setAttribute("id", exercise.getID());
				if (!exercise.getTheme().isEmpty())
					exNode.setAttribute("theme", exercise.getTheme());
				if (exercise.getType() == Exercise.Type.SQL)
					exNode.setAttribute("type", "sql");
				reader = new StringReader("<question>" + exercise.getQuestion()
						+ "</question>");
				tmpDoc = builder.parse(new InputSource(reader));
				Node newNode = doc.importNode(tmpDoc.getFirstChild(), true);
				exNode.appendChild(newNode);
				if (!exercise.getAnswer().isEmpty()) {
					newNode = doc.createElement("answer");
					newNode.setTextContent(exercise.getAnswer());
					exNode.appendChild(newNode);
				}
				if (!exercise.getSolution().isEmpty()) {
					newNode = doc.createElement("solution");
					newNode.setTextContent(exercise.getSolution());
					exNode.appendChild(newNode);
				}
			}
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
					"exercise-set.dtd");
			DOMSource source = new DOMSource(doc);
			StreamResult resultStream = new StreamResult(file);
			transformer.transform(source, resultStream);
		} catch (Exception e) {
			PersistenceException pe = new PersistenceException(
					"Writing exercise set to file " + file + " failed.", e);
			throw pe;
		}
	}

	/**
	 * Computes a hash value for editing information and answers. The result is
	 * used to check the integrity of loaded exercise sets.
	 */
	private String computeHashValue(ExerciseSet exercises) {
		String result = "";
		try {
			StringBuffer str = new StringBuffer(exercises.getEditor());
			str.append(exercises.getLastEdit());
			for (int i = 0; i < exercises.size(); i++)
				str.append(exercises.getExercise(i).getAnswer());
			result = SecureHashService.getInstance().encrypt(str.toString());
		} catch (EncryptionException e) {
			PersistenceException pe = new PersistenceException(
					"Could not compute hash value.", e);
			ErrorHandler.getInstance().handleError(pe);
		}
		return result;
	}

	/**
	 * Creates a HTML representation for a given XML file.
	 * 
	 * @param xmlFile
	 *            Source file containing exercise set data.
	 * @param htmlFile
	 *            Destination file.
	 * @param withSolutions
	 *            Controls whether solutions are also printed out.
	 * @throws PersistenceException
	 */
	public void transformExerciseSetToHTML(File xmlFile, File htmlFile,
			boolean withSolutions) throws PersistenceException {
		try {
			String xsl = (withSolutions) ? "exercise-set-with-solutions.xsl"
					: "exercise-set.xsl";
			StreamSource stylesheet = new StreamSource(new FileInputStream(
					new File(DTD_PATH, xsl)));
			StreamSource src = new StreamSource(new FileInputStream(xmlFile));
			src.setSystemId(DTD_PATH);
			StreamResult result = new StreamResult(htmlFile);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer t = factory.newTransformer(stylesheet);
			t.transform(src, result);
		} catch (Exception e) {
			PersistenceException pe = new PersistenceException(
					"Generation of the HTML file " + htmlFile + " failed.", e);
			throw pe;
		}
	}

	/**
	 * Helps the XML parser to find locally stored DTDs.
	 * 
	 * @author R. Lunde
	 */
	public class MyEntityResolver implements EntityResolver {
		public InputSource resolveEntity(String publicId, String systemId)
				throws FileNotFoundException {
			if (systemId.contains(".dtd")) {
				// System.out.println("publicId=" + publicId + " systemId=" +
				// systemId);
				File file = new File(DTD_PATH, systemId.substring(systemId
						.lastIndexOf('/')));
				return new InputSource(new FileInputStream(file));
			} else {
				// System.out.println("Other tag found");
				return null;
			}
		}
	}

	/**
	 * Simple handler for XML parser exceptions.
	 * 
	 * @author R. Lunde
	 */
	private static class MyErrorHandler implements org.xml.sax.ErrorHandler {
		public void warning(SAXParseException ex) {
			rl.util.exceptions.ErrorHandler.getInstance().handleWarning(ex);
		}

		public void error(SAXParseException ex) throws SAXException {
			throw ex;
		}

		public void fatalError(SAXParseException ex) throws SAXException {
			throw ex;
		}
	}

	// public static void main(String[] args) {
	// try {
	// ExerciseSetFileHandler el = new ExerciseSetFileHandler();
	// ExerciseSet es = el.loadExerciseSet(new
	// File("exercises/ExerciseSet1.xml"));
	// System.out.print(es);
	// } catch (ParserConfigurationException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (SAXException e) {
	// e.printStackTrace();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
}
