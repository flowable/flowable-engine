/**
 * Copyright (c) 2006
 * 
 * Philipp Berger, Martin Czuchra, Gero Decker, Ole Eckermann, Lutz Gericke,
 * Alexander Hold, Alexander Koglin, Oliver Kopp, Stefan Krumnow, 
 * Matthias Kunze, Philipp Maschke, Falko Menge, Christoph Neijenhuis, 
 * Hagen Overdick, Zhen Peng, Nicolas Peters, Kerstin Pfitzner, Daniel Polak,
 * Steffen Ryll, Kai Schlichting, Jan-Felix Schwarz, Daniel Taschik, 
 * Willi Tscheschner, Björn Wagner, Sven Wagner-Boysen, Matthias Weidlich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 **/
 
/**
 * @author nicolas.peters
 * @translation to spanish alfredo suarez
 * Contains all strings for the language (es-mx).
 * Version 1 - 08/29/08
 */
if(!ORYX) var ORYX = {};

if(!ORYX.I18N) ORYX.I18N = {};

ORYX.I18N.Language = "es_mx"; //Pattern <ISO language code>_<ISO country code> in lower case!

if(!ORYX.I18N.Oryx) ORYX.I18N.Oryx = {};

ORYX.I18N.Oryx.title		= "Oryx";
ORYX.I18N.Oryx.noBackendDefined	= "Precaucion! \nNo se definio Backend.\n El modelo requerido no pudo ser cargado. Intenta cargar la configuración con el plugin de guardado.";
ORYX.I18N.Oryx.pleaseWait 	= "Por favor espera mientras carga...";
ORYX.I18N.Oryx.notLoggedOn = "No ha iniciado sesion";
ORYX.I18N.Oryx.editorOpenTimeout = "El editor parece no haber iniciado aun. Por favor verifica, si tienes habiltado un bloqueador de popups, deshabilitado o permite los popups para este sitio. Nunca mostraremos anuncios en este sitio.";

if(!ORYX.I18N.AddDocker) ORYX.I18N.AddDocker = {};

ORYX.I18N.AddDocker.group = "Docker";
ORYX.I18N.AddDocker.add = "Agregar Docker";
ORYX.I18N.AddDocker.addDesc = "Agregar Docker a un borde, dando click en el";
ORYX.I18N.AddDocker.del = "Borrar Docker";
ORYX.I18N.AddDocker.delDesc = "Borrar un Docker";

if(!ORYX.I18N.Arrangement) ORYX.I18N.Arrangement = {};

ORYX.I18N.Arrangement.groupZ = "Z-Orden";
ORYX.I18N.Arrangement.btf = "Traer al frente";
ORYX.I18N.Arrangement.btfDesc = "Traer al frente";
ORYX.I18N.Arrangement.btb = "Enviar al fondo";
ORYX.I18N.Arrangement.btbDesc = "Enviar al fondo";
ORYX.I18N.Arrangement.bf = "Adelantar";
ORYX.I18N.Arrangement.bfDesc = "Adelantar";
ORYX.I18N.Arrangement.bb = "Atrasar";
ORYX.I18N.Arrangement.bbDesc = "Atrasar";
ORYX.I18N.Arrangement.groupA = "Alineacion";
ORYX.I18N.Arrangement.ab = "Alineacion inferior";
ORYX.I18N.Arrangement.abDesc = "Inferior";
ORYX.I18N.Arrangement.am = "Alineacion media";
ORYX.I18N.Arrangement.amDesc = "Media";
ORYX.I18N.Arrangement.at = "Alineacion Superior";
ORYX.I18N.Arrangement.atDesc = "Superior";
ORYX.I18N.Arrangement.al = "Alineacion izquierda";
ORYX.I18N.Arrangement.alDesc = "Izquierda";
ORYX.I18N.Arrangement.ac = "Alineacion central";
ORYX.I18N.Arrangement.acDesc = "Central";
ORYX.I18N.Arrangement.ar = "Alineacion derecha";
ORYX.I18N.Arrangement.arDesc = "Derecha";
ORYX.I18N.Arrangement.as = "Alineacion al mismo tamaño";
ORYX.I18N.Arrangement.asDesc = "Al mismo tamaño";

if(!ORYX.I18N.Edit) ORYX.I18N.Edit = {};

ORYX.I18N.Edit.group = "Editar";
ORYX.I18N.Edit.cut = "Cortar";
ORYX.I18N.Edit.cutDesc = "Corta la seleccion en un portapapeles Orix";
ORYX.I18N.Edit.copy = "Copiar";
ORYX.I18N.Edit.copyDesc = "Copia la seleccion en un portapapeles Orix";
ORYX.I18N.Edit.paste = "Pegar";
ORYX.I18N.Edit.pasteDesc = "Pega the el contenido del portapapeles Orix en el area de trabajo";
ORYX.I18N.Edit.del = "Borrar";
ORYX.I18N.Edit.delDesc = "Borra todas las formas seleccionadas";

if(!ORYX.I18N.EPCSupport) ORYX.I18N.EPCSupport = {};

ORYX.I18N.EPCSupport.group = "EPC";
ORYX.I18N.EPCSupport.exp = "Exportar EPC";
ORYX.I18N.EPCSupport.expDesc = "Exportar diagrama a EPML";
ORYX.I18N.EPCSupport.imp = "Importar EPC";
ORYX.I18N.EPCSupport.impDesc = "Importar un archivo EPML";
ORYX.I18N.EPCSupport.progressExp = "Exportando modelo";
ORYX.I18N.EPCSupport.selectFile = "Selecciona un archivo EPML (.empl) para importar.";
ORYX.I18N.EPCSupport.file = "Archivo";
ORYX.I18N.EPCSupport.impPanel = "Importar archivo EPML";
ORYX.I18N.EPCSupport.impBtn = "Importar";
ORYX.I18N.EPCSupport.close = "Cerrar";
ORYX.I18N.EPCSupport.error = "Error";
ORYX.I18N.EPCSupport.progressImp = "Importando...";

if(!ORYX.I18N.ERDFSupport) ORYX.I18N.ERDFSupport = {};

ORYX.I18N.ERDFSupport.exp = "Exportar a ERDF";
ORYX.I18N.ERDFSupport.expDesc = "Exportar a ERDF";
ORYX.I18N.ERDFSupport.imp = "Importar desde ERDF";
ORYX.I18N.ERDFSupport.impDesc = "Importar desde ERDF";
ORYX.I18N.ERDFSupport.impFailed = "Fallo la peticion para importar ERDF.";
ORYX.I18N.ERDFSupport.impFailed2 = "Ocurrio un erro mientras se importaba! <br/>Por favor revisa el mensaje de error: <br/><br/>";
ORYX.I18N.ERDFSupport.error = "Error";
ORYX.I18N.ERDFSupport.noCanvas = "El documento xml no tiene un nodo Oryx canvas incluido!";
ORYX.I18N.ERDFSupport.noSS = "El nodo Oryx canvas no incluye un stencil set definition!";
ORYX.I18N.ERDFSupport.wrongSS = "El stencil set proporcionado no encaja con el editor actual!";
ORYX.I18N.ERDFSupport.selectFile = "Selecciona un archivo ERDF (.xml) o escribe en el ERDF para importarlo!";
ORYX.I18N.ERDFSupport.file = "Archivo";
ORYX.I18N.ERDFSupport.impERDF = "Importar ERDF";
ORYX.I18N.ERDFSupport.impBtn = "Importar";
ORYX.I18N.ERDFSupport.impProgress = "Importando...";
ORYX.I18N.ERDFSupport.close = "Cerrar";
ORYX.I18N.ERDFSupport.deprTitle = "Realmente exportar a ERDF?";
ORYX.I18N.ERDFSupport.deprText = "Exportar a ERDF no es recomendable ya que no tendra soporte en versiones posteriores de Orix. Si es posible exporta el modelo a JSON. Aun asi deseas exportarlo?";

if(!ORYX.I18N.jPDLSupport) ORYX.I18N.jPDLSupport = {};

ORYX.I18N.jPDLSupport.group = "ExecBPMN";
ORYX.I18N.jPDLSupport.exp = "Exportar a jPDL";
ORYX.I18N.jPDLSupport.expDesc = "Exportar a jPDL";
ORYX.I18N.jPDLSupport.imp = "Importar desde jPDL";
ORYX.I18N.jPDLSupport.impDesc = "Importar jPDL File";
ORYX.I18N.jPDLSupport.impFailedReq = "Fallo la peticion para Importar jPDL.";
ORYX.I18N.jPDLSupport.impFailedJson = "Fallo la transformacion de jPDL.";
ORYX.I18N.jPDLSupport.impFailedJsonAbort = "Importacion cancelada.";
ORYX.I18N.jPDLSupport.loadSseQuestionTitle = "jBPM stencil set extension necesita ser cargada"; 
ORYX.I18N.jPDLSupport.loadSseQuestionBody = "Para Importar jPDL, la extension stencil set .debe ser cargada. Deseas continuar?";
ORYX.I18N.jPDLSupport.expFailedReq = "Fallo la peticion para exportar el modelo.";
ORYX.I18N.jPDLSupport.expFailedXml = "Fallo al exportar a jPDL. Exportador reporto: ";
ORYX.I18N.jPDLSupport.error = "Error";
ORYX.I18N.jPDLSupport.selectFile = "Selecciona un archivo jPDL (.xml) o escribe el jPDL a Importar!";
ORYX.I18N.jPDLSupport.file = "Archivo";
ORYX.I18N.jPDLSupport.impJPDL = "Importar jPDL";
ORYX.I18N.jPDLSupport.impBtn = "Importar";
ORYX.I18N.jPDLSupport.impProgress = "Importando...";
ORYX.I18N.jPDLSupport.close = "Cerrar";

if(!ORYX.I18N.Save) ORYX.I18N.Save = {};

ORYX.I18N.Save.group = "Archivo";
ORYX.I18N.Save.save = "Guardar";
ORYX.I18N.Save.saveDesc = "Guardar";
ORYX.I18N.Save.saveAs = "Guardar como...";
ORYX.I18N.Save.saveAsDesc = "Guardar como...";
ORYX.I18N.Save.unsavedData = "There are unsaved data, please save before you leave, otherwise your changes get lost!";
ORYX.I18N.Save.newProcess = "New Process";
ORYX.I18N.Save.saveAsTitle = "Guardar como...";
ORYX.I18N.Save.saveBtn = "Guardar";
ORYX.I18N.Save.close = "Cerrar";
ORYX.I18N.Save.savedAs = "Guardado como";
ORYX.I18N.Save.saved = "Guardado!";
ORYX.I18N.Save.failed = "Fallo al guardar.";
ORYX.I18N.Save.noRights = "No tienes privilegios para guardar cambios.";
ORYX.I18N.Save.saving = "Guardando";
ORYX.I18N.Save.saveAsHint = "El diagrama de proceso se almaceno bajo:";

if(!ORYX.I18N.File) ORYX.I18N.File = {};

ORYX.I18N.File.group = "Archivo";
ORYX.I18N.File.print = "Imprimir";
ORYX.I18N.File.printDesc = "Imprimir modelo actual";
ORYX.I18N.File.pdf = "Exportar como PDF";
ORYX.I18N.File.pdfDesc = "Exportar como PDF";
ORYX.I18N.File.info = "Info";
ORYX.I18N.File.infoDesc = "Info";
ORYX.I18N.File.genPDF = "Generando PDF";
ORYX.I18N.File.genPDFFailed = "Fallo generando PDF.";
ORYX.I18N.File.printTitle = "Imprimir";
ORYX.I18N.File.printMsg = "Estamos experimentando problemas con la impresion. Recomendamos utilizar el exportador PDF e imprimir el diagrama. Deseas continuar imprimiendo?";

if(!ORYX.I18N.Grouping) ORYX.I18N.Grouping = {};

ORYX.I18N.Grouping.grouping = "Agrupacion";
ORYX.I18N.Grouping.group = "Grupo";
ORYX.I18N.Grouping.groupDesc = "Agrupa todas las formas seleccionadas";
ORYX.I18N.Grouping.ungroup = "Desagrupar";
ORYX.I18N.Grouping.ungroupDesc = "Borra el grupo de todas las formas seleccionadas";

if(!ORYX.I18N.Loading) ORYX.I18N.Loading = {};

ORYX.I18N.Loading.waiting ="Por favor espera...";

if(!ORYX.I18N.PropertyWindow) ORYX.I18N.PropertyWindow = {};

ORYX.I18N.PropertyWindow.name = "Nombre";
ORYX.I18N.PropertyWindow.value = "Valor";
ORYX.I18N.PropertyWindow.selected = "seleccionado";
ORYX.I18N.PropertyWindow.clickIcon = "Click Icon";
ORYX.I18N.PropertyWindow.add = "Agregar";
ORYX.I18N.PropertyWindow.rem = "Remover";
ORYX.I18N.PropertyWindow.complex = "Editor para un tipo Complex";
ORYX.I18N.PropertyWindow.text = "Editor para un tipo Texto";
ORYX.I18N.PropertyWindow.ok = "Ok";
ORYX.I18N.PropertyWindow.cancel = "Cancelar";
ORYX.I18N.PropertyWindow.dateFormat = "m/d/y";

if(!ORYX.I18N.ShapeMenuPlugin) ORYX.I18N.ShapeMenuPlugin = {};

ORYX.I18N.ShapeMenuPlugin.drag = "Arrastrar";
ORYX.I18N.ShapeMenuPlugin.clickDrag = "Click o arrastra";
ORYX.I18N.ShapeMenuPlugin.morphMsg = "Transformar forma";

if(!ORYX.I18N.SyntaxChecker) ORYX.I18N.SyntaxChecker = {};

ORYX.I18N.SyntaxChecker.group = "Verificacion";
ORYX.I18N.SyntaxChecker.name = "Verificador sintactico";
ORYX.I18N.SyntaxChecker.desc = "Verificar sintaxis";
ORYX.I18N.SyntaxChecker.noErrors = "No hay errores de sintaxis.";
ORYX.I18N.SyntaxChecker.invalid = "Respuesta invalida desde el servidor.";
ORYX.I18N.SyntaxChecker.checkingMessage = "Verificando ...";

if(!ORYX.I18N.FormHandler) ORYX.I18N.FormHandler = {};

ORYX.I18N.FormHandler.group = "FormHandling";
ORYX.I18N.FormHandler.name = "FormHandler";
ORYX.I18N.FormHandler.desc = "Testing desde manejador";

if(!ORYX.I18N.Deployer) ORYX.I18N.Deployer = {};

ORYX.I18N.Deployer.group = "Despliegue";
ORYX.I18N.Deployer.name = "Deployer";
ORYX.I18N.Deployer.desc = "Desplegar al motor";

if(!ORYX.I18N.Tester) ORYX.I18N.Tester = {};

ORYX.I18N.Tester.group = "Testing";
ORYX.I18N.Tester.name = "Test process";
ORYX.I18N.Tester.desc = "Abre ek componente de test para probar esta definicion de procesos";

if(!ORYX.I18N.Undo) ORYX.I18N.Undo = {};

ORYX.I18N.Undo.group = "Deshacer";
ORYX.I18N.Undo.undo = "Deshacer";
ORYX.I18N.Undo.undoDesc = "Deshacer la ultima accion";
ORYX.I18N.Undo.redo = "Rehacer";
ORYX.I18N.Undo.redoDesc = "Rehacer la ultima accion deshecha";

if(!ORYX.I18N.View) ORYX.I18N.View = {};

ORYX.I18N.View.group = "Zoom";
ORYX.I18N.View.zoomIn = "Zoom In";
ORYX.I18N.View.zoomInDesc = "Zoom dentro del modelo";
ORYX.I18N.View.zoomOut = "Zoom Out";
ORYX.I18N.View.zoomOutDesc = "Zoom fuera del modelo";
ORYX.I18N.View.zoomStandard = "Zoom Standard";
ORYX.I18N.View.zoomStandardDesc = "Zoom al nivel standard";
ORYX.I18N.View.zoomFitToModel = "Zoom ajustado al modelo";
ORYX.I18N.View.zoomFitToModelDesc = "Zoom ajustado al tamaño del modelo";

if(!ORYX.I18N.XFormsSerialization) ORYX.I18N.XFormsSerialization = {};

ORYX.I18N.XFormsSerialization.group = "XForms Serializacion";
ORYX.I18N.XFormsSerialization.exportXForms = "XForms Exportacion";
ORYX.I18N.XFormsSerialization.exportXFormsDesc = "Exportar XForms+XHTML markup";
ORYX.I18N.XFormsSerialization.importXForms = "XForms Importar";
ORYX.I18N.XFormsSerialization.importXFormsDesc = "Importar XForms+XHTML markup";
ORYX.I18N.XFormsSerialization.noClientXFormsSupport = "No hay soporte para XForms ";
ORYX.I18N.XFormsSerialization.noClientXFormsSupportDesc = "<h2>Tu navegador no soporta XForms. Por favor instala la extension <a href=\"https://addons.mozilla.org/firefox/addon/824\" target=\"_blank\">Mozilla XForms </a> para Firefox.</h2>";
ORYX.I18N.XFormsSerialization.ok = "Ok";
ORYX.I18N.XFormsSerialization.selectFile = "Selecciona un archivo XHTML (.xhtml) o escribe el XForms+XHTML para importarlo!";
ORYX.I18N.XFormsSerialization.selectCss = "Por favor inserta la url del archivo css";
ORYX.I18N.XFormsSerialization.file = "Archivo";
ORYX.I18N.XFormsSerialization.impFailed = "Fallo la peticion para importar documento.";
ORYX.I18N.XFormsSerialization.impTitle = "Importar documento XForms+XHTML";
ORYX.I18N.XFormsSerialization.expTitle = "Exportar docuemento XForms+XHTML";
ORYX.I18N.XFormsSerialization.impButton = "Importar";
ORYX.I18N.XFormsSerialization.impProgress = "Importando...";
ORYX.I18N.XFormsSerialization.close = "Cerrar";

/** New Language Properties: 08.12.2008 */

ORYX.I18N.PropertyWindow.title = "Propiedades";

if(!ORYX.I18N.ShapeRepository) ORYX.I18N.ShapeRepository = {};
ORYX.I18N.ShapeRepository.title = "Shape Repository";

ORYX.I18N.Save.dialogDesciption = "Por favor introduce un nombre, una descripcion y un comentario.";
ORYX.I18N.Save.dialogLabelTitle = "Titulo";
ORYX.I18N.Save.dialogLabelDesc = "Descripcion";
ORYX.I18N.Save.dialogLabelType = "Tipo";
ORYX.I18N.Save.dialogLabelComment = "Comentario de revision";

if(!ORYX.I18N.Perspective) ORYX.I18N.Perspective = {};
ORYX.I18N.Perspective.no = "No hay perspectiva"
ORYX.I18N.Perspective.noTip = "Descargar la perspectiva actual"

/** New Language Properties: 21.04.2009 */
ORYX.I18N.JSONSupport = {
    imp: {
        name: "Importar desde JSON",
        desc: "Importa un modelo desde JSON",
        group: "Exportar",
        selectFile: "Selecciona un archivo JSON (.json) o escribe el JSON para importarlo!",
        file: "Archivo",
        btnImp: "Importar",
        btnClose: "Cerrar",
        progress: "Importando ...",
        syntaxError: "Error de sintaxis"
    },
    exp: {
        name: "Exportar a JSON",
        desc: "Exporta el modelo actual a JSON",
        group: "Exportar"
    }
};

/** New Language Properties: 09.05.2009 */
if(!ORYX.I18N.JSONImport) ORYX.I18N.JSONImportar = {};

ORYX.I18N.JSONImport.title = "Importacion JSON";
ORYX.I18N.JSONImport.wrongSS = "El stencil set del archivo importado ({0}) no encaja con el stencil set cargado ({1})."

/** New Language Properties: 14.05.2009 */
if(!ORYX.I18N.RDFExport) ORYX.I18N.RDFExport = {};
ORYX.I18N.RDFExport.group = "Exportar";
ORYX.I18N.RDFExport.rdfExport = "Exportar a RDF";
ORYX.I18N.RDFExport.rdfExportDescription = "Exporta el modelo actual a la serializacion  XML definida para el Resource Description Framework (RDF)";

/** New Language Properties: 15.05.2009*/
if(!ORYX.I18N.SyntaxChecker.BPMN) ORYX.I18N.SyntaxChecker.BPMN={};
ORYX.I18N.SyntaxChecker.BPMN_NO_SOURCE = "Un borde debe tener un origen.";
ORYX.I18N.SyntaxChecker.BPMN_NO_TARGET = "Un borde debe tener un destino.";
ORYX.I18N.SyntaxChecker.BPMN_DIFFERENT_PROCESS = "Nodo origen y destino deben estar contenidos en el mismo proceso.";
ORYX.I18N.SyntaxChecker.BPMN_SAME_PROCESS = "Nodo origen y destino deben estar contenidos en diferentes pools.";
ORYX.I18N.SyntaxChecker.BPMN_FLOWOBJECT_NOT_CONTAINED_IN_PROCESS = "Un objeto de flujo debe estar contenido en un proceso.";
ORYX.I18N.SyntaxChecker.BPMN_ENDEVENT_WITHOUT_INCOMING_CONTROL_FLOW = "Un evento de finalizacion debe tener un flujo de entrada.";
ORYX.I18N.SyntaxChecker.BPMN_STARTEVENT_WITHOUT_OUTGOING_CONTROL_FLOW = "Un evento de inicio debe tener un flujo de salida.";
ORYX.I18N.SyntaxChecker.BPMN_STARTEVENT_WITH_INCOMING_CONTROL_FLOW = "Eventos de inicio no deben tener un flujo de entrada.";
ORYX.I18N.SyntaxChecker.BPMN_ATTACHEDINTERMEDIATEEVENT_WITH_INCOMING_CONTROL_FLOW = "Eventos intermedios adjuntos no deben tener flujos de entrada";
ORYX.I18N.SyntaxChecker.BPMN_ATTACHEDINTERMEDIATEEVENT_WITHOUT_OUTGOING_CONTROL_FLOW = "Eventos intermedios adjuntos deben tener exactamente un flujo de salida.";
ORYX.I18N.SyntaxChecker.BPMN_ENDEVENT_WITH_OUTGOING_CONTROL_FLOW = "Eventos de finalizacion no deben tener flujos de salida.";
ORYX.I18N.SyntaxChecker.BPMN_EVENTBASEDGATEWAY_BADCONTINUATION = "Entradas de decision basadas en eventos no debe ser seguidas por entradas de decision o subprocesos.";
ORYX.I18N.SyntaxChecker.BPMN_NODE_NOT_ALLOWED = "El tipo de nodo no es permitido.";

if(!ORYX.I18N.SyntaxChecker.IBPMN) ORYX.I18N.SyntaxChecker.IBPMN={};
ORYX.I18N.SyntaxChecker.IBPMN_NO_ROLE_SET = "Interacciones deben tener afijados los roles emisor y receptor";
ORYX.I18N.SyntaxChecker.IBPMN_NO_INCOMING_SEQFLOW = "Este nodo debe tener un flujo de entrada.";
ORYX.I18N.SyntaxChecker.IBPMN_NO_OUTGOING_SEQFLOW = "Este nodo debe tener un flujo de salida.";

if(!ORYX.I18N.SyntaxChecker.InteractionNet) ORYX.I18N.SyntaxChecker.InteractionNet={};
ORYX.I18N.SyntaxChecker.InteractionNet_emisor_NOT_SET = "Emisor no fijado";
ORYX.I18N.SyntaxChecker.InteractionNet_receptor_NOT_SET = "Receptor no fijado";
ORYX.I18N.SyntaxChecker.InteractionNet_MESSAGETYPE_NOT_SET = "Tipo de mensaje no fijado";
ORYX.I18N.SyntaxChecker.InteractionNet_ROLE_NOT_SET = "Rol no fijado";

if(!ORYX.I18N.SyntaxChecker.EPC) ORYX.I18N.SyntaxChecker.EPC={};
ORYX.I18N.SyntaxChecker.EPC_NO_SOURCE = "Cada borde debe tener un origen.";
ORYX.I18N.SyntaxChecker.EPC_NO_TARGET = "Cada borde debe tener un destino.";
ORYX.I18N.SyntaxChecker.EPC_NOT_CONNECTED = "Un nodo debe estar conectado con bordes.";
ORYX.I18N.SyntaxChecker.EPC_NOT_CONNECTED_2 = "Un node debe estar conectado con mas bordes.";
ORYX.I18N.SyntaxChecker.EPC_TOO_MANY_EDGES = "El nodo tiene muchos bordes conectados.";
ORYX.I18N.SyntaxChecker.EPC_NO_CORRECT_CONNECTOR = "El nodo no es un conector correcto.";
ORYX.I18N.SyntaxChecker.EPC_MANY_STARTS = "Solo debe haber un evento de inicio.";
ORYX.I18N.SyntaxChecker.EPC_FUNCTION_AFTER_OR = "No debe haber funciones depues de partir un OR/XOR.";
ORYX.I18N.SyntaxChecker.EPC_PI_AFTER_OR = "TNo debe haber interfaces de proceo despues de partir un OR/XOR.";
ORYX.I18N.SyntaxChecker.EPC_FUNCTION_AFTER_FUNCTION =  "No debe haber una funcion despues de una funcion.";
ORYX.I18N.SyntaxChecker.EPC_EVENT_AFTER_EVENT =  "No debe haber un evento despues de un evento.";
ORYX.I18N.SyntaxChecker.EPC_PI_AFTER_FUNCTION =  "No debe haber una interface de proceso despues de una funcion.";
ORYX.I18N.SyntaxChecker.EPC_FUNCTION_AFTER_PI =  "No debe haber una funcion despues de una interfaces de proceso.";
ORYX.I18N.SyntaxChecker.EPC_SOURCE_EQUALS_TARGET = "Un borde debe conectar dos nodos distintos."

if(!ORYX.I18N.SyntaxChecker.PetriNet) ORYX.I18N.SyntaxChecker.PetriNet={};
ORYX.I18N.SyntaxChecker.PetriNet_NOT_BIPARTITE = "The graph is not bipartite";
ORYX.I18N.SyntaxChecker.PetriNet_NO_LABEL = "Etiqueta no fijada para una transicion etiquetada";
ORYX.I18N.SyntaxChecker.PetriNet_NO_ID = "Existe un nodo sin identificador";
ORYX.I18N.SyntaxChecker.PetriNet_SAME_SOURCE_AND_TARGET = "Dos relaciones de flujo tienen el mismo origen  y destino";
ORYX.I18N.SyntaxChecker.PetriNet_NODE_NOT_SET = "No se fijo un nodo para la relacion de flujo";

/** New Language Properties: 02.06.2009*/
ORYX.I18N.Edge = "Borde";
ORYX.I18N.Node = "Nodo";

/** New Language Properties: 03.06.2009*/
ORYX.I18N.SyntaxChecker.notice = "Mueve el mouse sobre el icono de la cruz roja para ver el mensaje de error.";

/** New Language Properties: 05.06.2009*/
if(!ORYX.I18N.RESIZE) ORYX.I18N.RESIZE = {};
ORYX.I18N.RESIZE.tipGrow = "Incrementar el tamaño del area de trabajo:";
ORYX.I18N.RESIZE.tipShrink = "Decrementar el tamaño de el area de trabajo:";
ORYX.I18N.RESIZE.N = "Parte superior";
ORYX.I18N.RESIZE.W = "Izquierda";
ORYX.I18N.RESIZE.S ="Abajo";
ORYX.I18N.RESIZE.E ="Derecha";

/** New Language Properties: 15.07.2009*/
if(!ORYX.I18N.Layouting) ORYX.I18N.Layouting ={};
ORYX.I18N.Layouting.doing = "Diseñando...";

/** New Language Properties: 18.08.2009*/
ORYX.I18N.SyntaxChecker.MULT_ERRORS = "Errores Multiples";

/** New Language Properties: 08.09.2009*/
if(!ORYX.I18N.PropertyWindow) ORYX.I18N.PropertyWindow = {};
ORYX.I18N.PropertyWindow.oftenUsed = "Usado con frecuencia";
ORYX.I18N.PropertyWindow.moreProps = "Mas Propiedades";

/** New Language Properties 01.10.2009 */
if(!ORYX.I18N.SyntaxChecker.BPMN2) ORYX.I18N.SyntaxChecker.BPMN2 = {};

ORYX.I18N.SyntaxChecker.BPMN2_DATA_INPUT_WITH_INCOMING_DATA_ASSOCIATION = "Una entrada de datos no debe tener ninguna  Asociacion de datos de entrada.";
ORYX.I18N.SyntaxChecker.BPMN2_DATA_OUTPUT_WITH_OUTGOING_DATA_ASSOCIATION = "Una salida de datos no debe tener ninguna Asociacion de datos de salida.";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_TARGET_WITH_TOO_MANY_INCOMING_SEQUENCE_FLOWS = "Los destinos de una toma de decision basada en eventos deben tener solo un flujo de entrada.";

/** New Language Properties 02.10.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WITH_TOO_LESS_OUTGOING_SEQUENCE_FLOWS = "Una toma de decision basada en eventos debe tener dos o mas flujos de salida.";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_EVENT_TARGET_CONTRADICTION = "Si mensajes intermedios de eventos son utilizados en la configuracion, entonces las tareas receptoras no deben ser utilizadas y  viceversa.";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WRONG_TRIGGER = "Only the following Intermediate Event triggers are valid: Message, Signal, Timer, Conditional and Multiple.";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WRONG_CONDITION_EXPRESSION = "El flujo de salidas de un evento de toma de decision no debe tener una expresion de condicion.";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_NOT_INSTANTIATING = "La toma de decision no cumple las condiciones para instanciar al proceso. Por favor utiliza un evento de inicio o un atributo instanciable de la toma de decision.";

/** New Language Properties 05.10.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAYDIRECTION_MIXED_FAILURE = "La toma de decision debe tener ambos flujos de entrada y de salida.";
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAYDIRECTION_CONVERGING_FAILURE = "La toma de decision debe tener multiples entradas pero no debe tenero multiples flujos de salida.";
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAYDIRECTION_DIVERGING_FAILURE = "La toma de decision no debe tener multiples entradas pero debe tener multiples flujos de salida.";
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAY_WITH_NO_OUTGOING_SEQUENCE_FLOW = "Una toma de decision debe tene al menos un flujo de salida.";
ORYX.I18N.SyntaxChecker.BPMN2_RECEIVE_TASK_WITH_ATTACHED_EVENT = "Las configuraciones de las tareas receptoras de el evento de toma de decision no deben tener ningun evento intermedio adjunto.";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_SUBPROCESS_BAD_CONNECTION = "Un evento de subproceso no debe tener ningun flujo de entrada o flujo de salida.";

/** New Language Properties 13.10.2009 */
ORYX.I18N.SyntaxChecker.BPMN_MESSAGE_FLOW_NOT_CONNECTED = "Al menos un lado del flujo del mensaje debe star conectado.";

/** New Language Properties 24.11.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_TOO_MANY_INITIATING_MESSAGES = "Una actividad de coreografia debe tener solo un mensaje de inicio.";
ORYX.I18N.SyntaxChecker.BPMN_MESSAGE_FLOW_NOT_ALLOWED = "Un flujo de mensaje no esta permitido aqui.";

/** New Language Properties 27.11.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WITH_TOO_LESS_INCOMING_SEQUENCE_FLOWS = "Una toma de desicion basada en evento que no esta siendo instanciada debe tener al menos un flujo de entrada.";
ORYX.I18N.SyntaxChecker.BPMN2_TOO_FEW_INITIATING_PARTICIPANTS = "Una actividad de coreografia deben tener un Participante iniciador (white).";
ORYX.I18N.SyntaxChecker.BPMN2_TOO_MANY_INITIATING_PARTICIPANTS = "Una actividad de coreografia no debe tener mas que un Participante iniciador (white)."

ORYX.I18N.SyntaxChecker.COMMUNICATION_AT_LEAST_TWO_PARTICIPANTS = "La comunicacion debe estar connectada al menos a dos participants.";
ORYX.I18N.SyntaxChecker.MESSAGEFLOW_START_MUST_BE_PARTICIPANT = "El origen del flujo de mensaje debe tener un participante.";
ORYX.I18N.SyntaxChecker.MESSAGEFLOW_END_MUST_BE_PARTICIPANT = "El destino del flujo de mensaje";
ORYX.I18N.SyntaxChecker.CONV_LINK_CANNOT_CONNECT_CONV_NODES = "El enlace de conversacion debe conectar un nodo de comunicacion o un nodo de sub conversacion con un participante.";
