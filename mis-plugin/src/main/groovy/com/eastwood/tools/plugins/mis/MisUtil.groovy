package com.eastwood.tools.plugins.mis

import com.android.build.gradle.BaseExtension
import com.eastwood.tools.plugins.mis.extension.MisSource
import org.gradle.api.Project
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class MisUtil {

    static MisSource getMisSourceFormManifest(Project project, String groupId, String artifactId) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        File misDir = new File(project.rootDir, '.gradle/mis')
        if (!misDir.exists()) {
            return null
        }

        File misSourceManifest = new File(misDir, 'misSourceManifest.xml')
        if (!misSourceManifest.exists()) {
            return null
        }

        Document document = builderFactory.newDocumentBuilder().parse(misSourceManifest)
        NodeList misSourceNodeList = document.getElementsByTagName("misSource")
        if (misSourceNodeList.length == 0) {
            return null
        }
        Element misSourceElement = (Element) misSourceNodeList.item(0)
        NodeList projectNodeList = misSourceElement.getElementsByTagName("project")
        for (int i = 0; i < projectNodeList.getLength(); i++) {
            Element projectElement = (Element) projectNodeList.item(i)
            def groupIdTemp = projectElement.getAttribute("groupId")
            def artifactIdTemp = projectElement.getAttribute("artifactId")
            if (groupId == groupIdTemp && artifactId == artifactIdTemp) {
                MisSource misSource = new MisSource()
                misSource.groupId = groupId
                misSource.artifactId = artifactId
                misSource.version = projectElement.getAttribute("version")
                misSource.invalid = Boolean.valueOf(projectElement.getAttribute("invalid"))
                return misSource
            }
        }
        return null
    }

    static updateMisSourceManifest(Project project, List<MisSource> misSourceList) {
        Map<String, MisSource> misSourceMap = new HashMap<>()
        misSourceList.each {
            misSourceMap.put(it.groupId + ":" + it.artifactId, it)
        }

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        File misDir = new File(project.rootDir, '.gradle/mis')
        if (!misDir.exists()) {
            misDir.mkdirs()
        }

        Document document
        Element misSourceElement

        File misSourceManifest = new File(misDir, 'misSourceManifest.xml')
        if (misSourceManifest.exists()) {
            document = builderFactory.newDocumentBuilder().parse(misSourceManifest)
            NodeList misSourceNodeList = document.getElementsByTagName("misSource")
            if (misSourceNodeList.length == 0) {
                misSourceElement = document.createElement("misSource")
            } else {
                misSourceElement = (Element) misSourceNodeList.item(0)
                NodeList projectNodeList = misSourceElement.getElementsByTagName("project")
                for (int i = 0; i < projectNodeList.getLength(); i++) {
                    Element projectElement = (Element) projectNodeList.item(i)
                    def groupId = projectElement.getAttribute("groupId")
                    def artifactId = projectElement.getAttribute("artifactId")

                    def key = groupId + ":" + artifactId
                    def options = misSourceMap.get(key)
                    if (options != null) {
                        misSourceMap.remove(key)
                        def version = projectElement.getAttribute("version")
                        if (version != options.version) {
                            projectElement.setAttribute("version", options.version)
                            projectElement.setAttribute('invalid', options.invalid ? "true": "false")
                        }
                    }
                }
            }
        } else {
            document = builderFactory.newDocumentBuilder().newDocument()
            misSourceElement = document.createElement("misSource")
        }

        misSourceMap.each {
            MisSource misSource = it.value
            Element projectElement = document.createElement('project')
            projectElement.setAttribute('groupId', misSource.groupId)
            projectElement.setAttribute('artifactId', misSource.artifactId)
            projectElement.setAttribute('version', misSource.version)
            projectElement.setAttribute('invalid', misSource.invalid ? "true": "false")
            misSourceElement.appendChild(projectElement)
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(new DOMSource(misSourceElement), new StreamResult(misSourceManifest))
    }

    static setProjectMisSourceDirs(Project project) {
        def type = "main"
        BaseExtension android = project.extensions.getByName('android')
        def obj = android.sourceSets.getByName(type)

        obj.java.srcDirs.each {
            obj.aidl.srcDirs(it.absolutePath.replace('java', 'mis'))
        }
    }

}