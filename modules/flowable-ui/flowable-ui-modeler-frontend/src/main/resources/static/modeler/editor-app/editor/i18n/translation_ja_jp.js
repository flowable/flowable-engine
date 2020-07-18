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
 * 
 * Contains all strings for the default language (en-us).
 * Version 1 - 08/29/08
 */
if(!ORYX) var ORYX = {};

if(!ORYX.I18N) ORYX.I18N = {};

ORYX.I18N.Language = "ja_jp"; //Pattern <ISO language code>_<ISO country code> in lower case!

if(!ORYX.I18N.Oryx) ORYX.I18N.Oryx = {};

ORYX.I18N.Oryx.title		= "Oryx";
ORYX.I18N.Oryx.noBackendDefined	= "注意! \nバックエンドが定義されていません。\n 要求されたモデルはロードできません。 保存プラグインを使って設定をロードしてください。";
ORYX.I18N.Oryx.pleaseWait 	= "しばらくお待ちください　ロード中...";
ORYX.I18N.Oryx.notLoggedOn = "ログオンしていません";
ORYX.I18N.Oryx.editorOpenTimeout = "エディターはまだ開始されていません。広告ブロッカーの設定などをご確認ください。このサイトでは広告は表示されません。";

if(!ORYX.I18N.AddDocker) ORYX.I18N.AddDocker = {};

ORYX.I18N.AddDocker.group = "Docker";
ORYX.I18N.AddDocker.add = "Docker追加";
ORYX.I18N.AddDocker.addDesc = "クリックしてエッジにDockerを追加";
ORYX.I18N.AddDocker.del = "Docker削除";
ORYX.I18N.AddDocker.delDesc = "Dockerを削除";

if(!ORYX.I18N.Arrangement) ORYX.I18N.Arrangement = {};

ORYX.I18N.Arrangement.groupZ = "Z-順";
ORYX.I18N.Arrangement.btf = "最前面に移動";
ORYX.I18N.Arrangement.btfDesc = "最前面に移動";
ORYX.I18N.Arrangement.btb = "最背面に移動";
ORYX.I18N.Arrangement.btbDesc = "最背面に移動";
ORYX.I18N.Arrangement.bf = "前へ移動";
ORYX.I18N.Arrangement.bfDesc = "前へ移動";
ORYX.I18N.Arrangement.bb = "後へ移動";
ORYX.I18N.Arrangement.bbDesc = "後へ移動";
ORYX.I18N.Arrangement.groupA = "整列";
ORYX.I18N.Arrangement.ab = "整列 下";
ORYX.I18N.Arrangement.abDesc = "下";
ORYX.I18N.Arrangement.am = "整列 中";
ORYX.I18N.Arrangement.amDesc = "中";
ORYX.I18N.Arrangement.at = "整列 上";
ORYX.I18N.Arrangement.atDesc = "上";
ORYX.I18N.Arrangement.al = "整列 左";
ORYX.I18N.Arrangement.alDesc = "左";
ORYX.I18N.Arrangement.ac = "整列 中央";
ORYX.I18N.Arrangement.acDesc = "中央";
ORYX.I18N.Arrangement.ar = "整列 右";
ORYX.I18N.Arrangement.arDesc = "右";
ORYX.I18N.Arrangement.as = "整列 同サイズ";
ORYX.I18N.Arrangement.asDesc = "同サイズ";

if(!ORYX.I18N.Edit) ORYX.I18N.Edit = {};

ORYX.I18N.Edit.group = "編集";
ORYX.I18N.Edit.cut = "カット";
ORYX.I18N.Edit.cutDesc = "Oryxクリップボードへカット";
ORYX.I18N.Edit.copy = "コピー";
ORYX.I18N.Edit.copyDesc = "Oryxクリップボードへコピー";
ORYX.I18N.Edit.paste = "ペースト";
ORYX.I18N.Edit.pasteDesc = "Oryxクリップボードからキャンバスへ貼り付け";
ORYX.I18N.Edit.del = "削除";
ORYX.I18N.Edit.delDesc = "選択した図形をすべて削除";

if(!ORYX.I18N.EPCSupport) ORYX.I18N.EPCSupport = {};

ORYX.I18N.EPCSupport.group = "EPC";
ORYX.I18N.EPCSupport.exp = "EPCエクスポート";
ORYX.I18N.EPCSupport.expDesc = "ダイアグラムをEPMLにエクスポート";
ORYX.I18N.EPCSupport.imp = "EPCインポート";
ORYX.I18N.EPCSupport.impDesc = "EPMLファイルをインポート";
ORYX.I18N.EPCSupport.progressExp = "モデルをエクスポート中";
ORYX.I18N.EPCSupport.selectFile = "EPML (.empl) ファイルを選択してください。";
ORYX.I18N.EPCSupport.file = "ファイル";
ORYX.I18N.EPCSupport.impPanel = "EPMLファイルインポート";
ORYX.I18N.EPCSupport.impBtn = "インポート";
ORYX.I18N.EPCSupport.close = "閉じる";
ORYX.I18N.EPCSupport.error = "エラー";
ORYX.I18N.EPCSupport.progressImp = "インポート中...";

if(!ORYX.I18N.ERDFSupport) ORYX.I18N.ERDFSupport = {};

ORYX.I18N.ERDFSupport.exp = "ERDFエクスポート";
ORYX.I18N.ERDFSupport.expDesc = "ERDFにエクスポート";
ORYX.I18N.ERDFSupport.imp = "ERDFインポート";
ORYX.I18N.ERDFSupport.impDesc = "ERDFからインポート";
ORYX.I18N.ERDFSupport.impFailed = "ERDFインポートのリクエストに失敗しました。";
ORYX.I18N.ERDFSupport.impFailed2 = "インポート中にエラーが発生しました。 <br/>エラーメッセージを確認してください。 <br/><br/>";
ORYX.I18N.ERDFSupport.error = "エラー";
ORYX.I18N.ERDFSupport.noCanvas = "XML文書内にOryxキャンバスのノードがありません。";
ORYX.I18N.ERDFSupport.noSS = "Oryxキャンバスのノードにステンシルセットの定義が含まれていません。";
ORYX.I18N.ERDFSupport.wrongSS = "ステンシルセットが現在のエディターにあっていません。";
ORYX.I18N.ERDFSupport.selectFile = "ERDF (.xml) ファイルを選択してください。";
ORYX.I18N.ERDFSupport.file = "ファイル";
ORYX.I18N.ERDFSupport.impERDF = "ERDFインポート";
ORYX.I18N.ERDFSupport.impBtn = "インポート";
ORYX.I18N.ERDFSupport.impProgress = "インポート中...";
ORYX.I18N.ERDFSupport.close = "閉じる";
ORYX.I18N.ERDFSupport.deprTitle = "eRDFにエクスポートしますか？";
ORYX.I18N.ERDFSupport.deprText = "Exporting to eRDFへのエクスポートは将来のバージョンのOryxエディターではサポートされなくなるため推奨されません。それでもエクスポートを実行しますか？ ";

if(!ORYX.I18N.jPDLSupport) ORYX.I18N.jPDLSupport = {};

ORYX.I18N.jPDLSupport.group = "BPMN実行";
ORYX.I18N.jPDLSupport.exp = "Export to jPDLエクスポート";
ORYX.I18N.jPDLSupport.expDesc = "jPDLにエクスポート";
ORYX.I18N.jPDLSupport.imp = "jPDLインポート";
ORYX.I18N.jPDLSupport.impDesc = "jPDLファイルをインポート";
ORYX.I18N.jPDLSupport.impFailedReq = "jPDLインポートのリクエストに失敗しました。";
ORYX.I18N.jPDLSupport.impFailedJson = "jPDLの変換に失敗しました。";
ORYX.I18N.jPDLSupport.impFailedJsonAbort = "インポートは中止されました。";
ORYX.I18N.jPDLSupport.loadSseQuestionTitle = "jBPMステンシルセット拡張をロードする必要があります。"; 
ORYX.I18N.jPDLSupport.loadSseQuestionBody = "jPDLインポートを実行するために、ステンシルセット拡張をロードする必要があります。処理を進めますか？";
ORYX.I18N.jPDLSupport.expFailedReq = "モデルエクスポートのリクエストに失敗しました。";
ORYX.I18N.jPDLSupport.expFailedXml = "jPDLエクスポートに失敗しました。 ";
ORYX.I18N.jPDLSupport.error = "エラー";
ORYX.I18N.jPDLSupport.selectFile = "jPDL (.xml) ファイルを選択してください。";
ORYX.I18N.jPDLSupport.file = "ファイル";
ORYX.I18N.jPDLSupport.impJPDL = "jPDLインポート";
ORYX.I18N.jPDLSupport.impBtn = "インポート";
ORYX.I18N.jPDLSupport.impProgress = "インポート中...";
ORYX.I18N.jPDLSupport.close = "閉じる";

if(!ORYX.I18N.Save) ORYX.I18N.Save = {};

ORYX.I18N.Save.group = "ファイル";
ORYX.I18N.Save.save = "保存";
ORYX.I18N.Save.saveDesc = "保存";
ORYX.I18N.Save.saveAs = "名前をつけて保存";
ORYX.I18N.Save.saveAsDesc = "名前をつけて保存";
ORYX.I18N.Save.unsavedData = "保存されていないデータがあります。画面を離れる前に保存をしてください。変更が失われます";
ORYX.I18N.Save.newProcess = "新規プロセス";
ORYX.I18N.Save.saveAsTitle = "名前をつけて保存";
ORYX.I18N.Save.saveBtn = "保存";
ORYX.I18N.Save.close = "閉じる";
ORYX.I18N.Save.savedAs = "名前をつけて保存";
ORYX.I18N.Save.saved = "保存されました。";
ORYX.I18N.Save.failed = "保存に失敗しました。";
ORYX.I18N.Save.noRights = "保存をするための権限がありません。";
ORYX.I18N.Save.saving = "保存中";
ORYX.I18N.Save.saveAsHint = "プロセスダイアグラムが保存されます。";

if(!ORYX.I18N.File) ORYX.I18N.File = {};

ORYX.I18N.File.group = "ファイル";
ORYX.I18N.File.print = "印刷";
ORYX.I18N.File.printDesc = "現在のモデルを印刷";
ORYX.I18N.File.pdf = "PDFエクスポート";
ORYX.I18N.File.pdfDesc = "PDFとしてエクスポート";
ORYX.I18N.File.info = "情報";
ORYX.I18N.File.infoDesc = "情報";
ORYX.I18N.File.genPDF = "PDF生成中";
ORYX.I18N.File.genPDFFailed = "PDF生成に失敗しました。";
ORYX.I18N.File.printTitle = "印刷";
ORYX.I18N.File.printMsg = "印刷機能には現在いくつかの問題が発見されているため、PDFエクスポート機能を推奨しています。印刷を実行しますか？";

if(!ORYX.I18N.Grouping) ORYX.I18N.Grouping = {};

ORYX.I18N.Grouping.grouping = "グループ化";
ORYX.I18N.Grouping.group = "グループ";
ORYX.I18N.Grouping.groupDesc = "選択した図形をグループ化";
ORYX.I18N.Grouping.ungroup = "グループ解除";
ORYX.I18N.Grouping.ungroupDesc = "選択した図形のグループを解除";

if(!ORYX.I18N.Loading) ORYX.I18N.Loading = {};

ORYX.I18N.Loading.waiting ="しばらくお待ちください...";

if(!ORYX.I18N.PropertyWindow) ORYX.I18N.PropertyWindow = {};

ORYX.I18N.PropertyWindow.name = "名前";
ORYX.I18N.PropertyWindow.value = "値";
ORYX.I18N.PropertyWindow.selected = "選択済";
ORYX.I18N.PropertyWindow.clickIcon = "アイコンクリック";
ORYX.I18N.PropertyWindow.add = "追加";
ORYX.I18N.PropertyWindow.rem = "削除";
ORYX.I18N.PropertyWindow.complex = "複雑な型のためのエディター";
ORYX.I18N.PropertyWindow.text = "テキスト型のためのエディター";
ORYX.I18N.PropertyWindow.ok = "OK";
ORYX.I18N.PropertyWindow.cancel = "キャンセル";
ORYX.I18N.PropertyWindow.dateFormat = "y/m/d";

if(!ORYX.I18N.ShapeMenuPlugin) ORYX.I18N.ShapeMenuPlugin = {};

ORYX.I18N.ShapeMenuPlugin.drag = "ドラッグ";
ORYX.I18N.ShapeMenuPlugin.clickDrag = "クリックかドラッグ";
ORYX.I18N.ShapeMenuPlugin.morphMsg = "図形の変形";

if(!ORYX.I18N.SyntaxChecker) ORYX.I18N.SyntaxChecker = {};

ORYX.I18N.SyntaxChecker.group = "検証";
ORYX.I18N.SyntaxChecker.name = "文法チェッカー";
ORYX.I18N.SyntaxChecker.desc = "文法チェック";
ORYX.I18N.SyntaxChecker.noErrors = "文法エラーはありません。";
ORYX.I18N.SyntaxChecker.invalid = "サーバーの応答が不正です。";
ORYX.I18N.SyntaxChecker.checkingMessage = "チェック中...";

if(!ORYX.I18N.FormHandler) ORYX.I18N.FormHandler = {};

ORYX.I18N.FormHandler.group = "フォームハンドリング";
ORYX.I18N.FormHandler.name = "フォームハンドラー";
ORYX.I18N.FormHandler.desc = "ハンドリングテスト";

if(!ORYX.I18N.Deployer) ORYX.I18N.Deployer = {};

ORYX.I18N.Deployer.group = "デプロイメント";
ORYX.I18N.Deployer.name = "デプロイヤー";
ORYX.I18N.Deployer.desc = "エンジンへのデプロイ";

if(!ORYX.I18N.Tester) ORYX.I18N.Tester = {};

ORYX.I18N.Tester.group = "テスト";
ORYX.I18N.Tester.name = "プロセスのテスト";
ORYX.I18N.Tester.desc = "このプロセス定義をテストするためにテストコンポーネントを開きます。";

if(!ORYX.I18N.Undo) ORYX.I18N.Undo = {};

ORYX.I18N.Undo.group = "取り消し/やり直し";
ORYX.I18N.Undo.undo = "取り消す";
ORYX.I18N.Undo.undoDesc = "最後のアクションを取り消す";
ORYX.I18N.Undo.redo = "やり直す";
ORYX.I18N.Undo.redoDesc = "取り消したアクションをやり直す";

if(!ORYX.I18N.View) ORYX.I18N.View = {};

ORYX.I18N.View.group = "拡大/縮小";
ORYX.I18N.View.zoomIn = "拡大";
ORYX.I18N.View.zoomInDesc = "モデルの拡大表示。ズームイン";
ORYX.I18N.View.zoomOut = "縮小t";
ORYX.I18N.View.zoomOutDesc = "モデルの縮小表示。ズームアウト";
ORYX.I18N.View.zoomStandard = "標準";
ORYX.I18N.View.zoomStandardDesc = "モデルの初期表示";
ORYX.I18N.View.zoomFitToModel = "モデルに合わせる";
ORYX.I18N.View.zoomFitToModelDesc = "モデルのサイズに合わせた表示";

if(!ORYX.I18N.XFormsSerialization) ORYX.I18N.XFormsSerialization = {};

ORYX.I18N.XFormsSerialization.group = "XFormsシリアライズ";
ORYX.I18N.XFormsSerialization.exportXForms = "XFormsエクスポート";
ORYX.I18N.XFormsSerialization.exportXFormsDesc = "XForms+XHTMLマークアップにエクスポート";
ORYX.I18N.XFormsSerialization.importXForms = "XFormsインポート";
ORYX.I18N.XFormsSerialization.importXFormsDesc = "XForms+XHTMLマークアップをインポート";
ORYX.I18N.XFormsSerialization.noClientXFormsSupport = "XFormsがサポートされていません。";
ORYX.I18N.XFormsSerialization.noClientXFormsSupportDesc = "<h2>あなたのブラウザはXFormsをサポートしていません。 <a href=\"https://addons.mozilla.org/firefox/addon/824\" target=\"_blank\">Mozilla XFormsアドオン</a> をFirefoxにインストールしてください。</h2>";
ORYX.I18N.XFormsSerialization.ok = "OK";
ORYX.I18N.XFormsSerialization.selectFile = "XHTML (.xhtml) ファイルを選択してください。";
ORYX.I18N.XFormsSerialization.selectCss = "CSSファイルのURLを挿入してください。";
ORYX.I18N.XFormsSerialization.file = "ファイル";
ORYX.I18N.XFormsSerialization.impFailed = "文書インポートのリクエストに失敗しました。";
ORYX.I18N.XFormsSerialization.impTitle = "XForms+XHTML文書インポート";
ORYX.I18N.XFormsSerialization.expTitle = "XForms+XHTML文書エクスポート";
ORYX.I18N.XFormsSerialization.impButton = "インポート";
ORYX.I18N.XFormsSerialization.impProgress = "インポート中...";
ORYX.I18N.XFormsSerialization.close = "閉じる";

/** New Language Properties: 08.12.2008 */

ORYX.I18N.PropertyWindow.title = "プロパティ";

if(!ORYX.I18N.ShapeRepository) ORYX.I18N.ShapeRepository = {};
ORYX.I18N.ShapeRepository.title = "図形リポジトリー";

ORYX.I18N.Save.dialogDesciption = "名前、説明、コメントを入力してください。";
ORYX.I18N.Save.dialogLabelTitle = "タイトル";
ORYX.I18N.Save.dialogLabelDesc = "説明";
ORYX.I18N.Save.dialogLabelType = "型（タイプ）";
ORYX.I18N.Save.dialogLabelComment = "版コメント";

if(!ORYX.I18N.Perspective) ORYX.I18N.Perspective = {};
ORYX.I18N.Perspective.no = "パースペクティブなし"
ORYX.I18N.Perspective.noTip = "現在のパースペクティブをアンロードします。"

/** New Language Properties: 21.04.2009 */
ORYX.I18N.JSONSupport = {
    imp: {
        name: "JSONインポート",
        desc: "JSONからモデルをインポート",
        group: "エクスポート",
        selectFile: "JSON (.json)ファイルを選択してください。",
        file: "File",
        btnImp: "インポート",
        btnClose: "閉じる",
        progress: "インポート中...",
        syntaxError: "文法エラー"
    },
    exp: {
        name: "SONエクスポート",
        desc: "現在のモデルをJSONにエクスポート",
        group: "エクスポート"
    }
};

/** New Language Properties: 09.05.2009 */
if(!ORYX.I18N.JSONImport) ORYX.I18N.JSONImport = {};

ORYX.I18N.JSONImport.title = "JSONインポート";
ORYX.I18N.JSONImport.wrongSS = "インポートファイル ({0})のステンシスセットはロードされているステンシルセット ({1})と合いません。"

/** New Language Properties: 14.05.2009 */
if(!ORYX.I18N.RDFExport) ORYX.I18N.RDFExport = {};
ORYX.I18N.RDFExport.group = "エクスポート";
ORYX.I18N.RDFExport.rdfExport = "RDFエクスポート";
ORYX.I18N.RDFExport.rdfExportDescription = "現在のモデルをResource Description Framework (RDF)で定義されたXMLシリアライズ形式にエクスポート";

/** New Language Properties: 15.05.2009*/
if(!ORYX.I18N.SyntaxChecker.BPMN) ORYX.I18N.SyntaxChecker.BPMN={};
ORYX.I18N.SyntaxChecker.BPMN_NO_SOURCE = "エッジにはソースが必要です。";
ORYX.I18N.SyntaxChecker.BPMN_NO_TARGET = "エッジにはターゲットが必要です。";
ORYX.I18N.SyntaxChecker.BPMN_DIFFERENT_PROCESS = "ソースとターゲットのノードは同じプロセスに含まれている必要があります。";
ORYX.I18N.SyntaxChecker.BPMN_SAME_PROCESS = "ソースとターゲットのノードは異なるプールに含まれている必要があります。";
ORYX.I18N.SyntaxChecker.BPMN_FLOWOBJECT_NOT_CONTAINED_IN_PROCESS = "プロセスにはフローオブジェクトが含まれている必要があります。";
ORYX.I18N.SyntaxChecker.BPMN_ENDEVENT_WITHOUT_INCOMING_CONTROL_FLOW = "終了イベントには入力シーケンスフローが必要です。";
ORYX.I18N.SyntaxChecker.BPMN_STARTEVENT_WITHOUT_OUTGOING_CONTROL_FLOW = "開始イベントには出力シーケンスフローが必要です。";
ORYX.I18N.SyntaxChecker.BPMN_STARTEVENT_WITH_INCOMING_CONTROL_FLOW = "開始イベントは入力シーケンスフローを持つことができません。";
ORYX.I18N.SyntaxChecker.BPMN_ATTACHEDINTERMEDIATEEVENT_WITH_INCOMING_CONTROL_FLOW = "境界の中間イベントは入力シーケンスフローを持つことができません。";
ORYX.I18N.SyntaxChecker.BPMN_ATTACHEDINTERMEDIATEEVENT_WITHOUT_OUTGOING_CONTROL_FLOW = "境界の中間イベントには出力シーケンスフローが必要です。";
ORYX.I18N.SyntaxChecker.BPMN_ENDEVENT_WITH_OUTGOING_CONTROL_FLOW = "終了イベントは出力シーケンスフローを持つことができません。";
ORYX.I18N.SyntaxChecker.BPMN_EVENTBASEDGATEWAY_BADCONTINUATION = "イベントベースゲートウェイはゲートウェイやサブプロセスで受けることができません。";
ORYX.I18N.SyntaxChecker.BPMN_NODE_NOT_ALLOWED = "ノードタイプが許可されません。";

if(!ORYX.I18N.SyntaxChecker.IBPMN) ORYX.I18N.SyntaxChecker.IBPMN={};
ORYX.I18N.SyntaxChecker.IBPMN_NO_ROLE_SET = "インタラクションには送信者/受信者のロールセットが必要です。";
ORYX.I18N.SyntaxChecker.IBPMN_NO_INCOMING_SEQFLOW = "このノードには入力シーケンスフローが必要です。";
ORYX.I18N.SyntaxChecker.IBPMN_NO_OUTGOING_SEQFLOW = "このノードには出力シーケンスフローが必要です。";

if(!ORYX.I18N.SyntaxChecker.InteractionNet) ORYX.I18N.SyntaxChecker.InteractionNet={};
ORYX.I18N.SyntaxChecker.InteractionNet_SENDER_NOT_SET = "送信者未設定";
ORYX.I18N.SyntaxChecker.InteractionNet_RECEIVER_NOT_SET = "受信者未設定";
ORYX.I18N.SyntaxChecker.InteractionNet_MESSAGETYPE_NOT_SET = "メッセージタイプ未設定";
ORYX.I18N.SyntaxChecker.InteractionNet_ROLE_NOT_SET = "ロール未設定";

if(!ORYX.I18N.SyntaxChecker.EPC) ORYX.I18N.SyntaxChecker.EPC={};
ORYX.I18N.SyntaxChecker.EPC_NO_SOURCE = "エッジにはソースが必要です。";
ORYX.I18N.SyntaxChecker.EPC_NO_TARGET = "エッジにはターゲットが必要です。";
ORYX.I18N.SyntaxChecker.EPC_NOT_CONNECTED = "ノードはエッジと接続している必要があります。";
ORYX.I18N.SyntaxChecker.EPC_NOT_CONNECTED_2 = "ノードに接続するエッジが不足しています。";
ORYX.I18N.SyntaxChecker.EPC_TOO_MANY_EDGES = "ノードに接続するエッジが多すぎます。";
ORYX.I18N.SyntaxChecker.EPC_NO_CORRECT_CONNECTOR = "適切なコネクタがありません。";
ORYX.I18N.SyntaxChecker.EPC_MANY_STARTS = "開始イベントは1つだけにする必要があります。";
ORYX.I18N.SyntaxChecker.EPC_FUNCTION_AFTER_OR = "OR/XOR分岐の後に関数を設定することはできません。";
ORYX.I18N.SyntaxChecker.EPC_PI_AFTER_OR = "OR/XOR分岐の後にプロセスインターフェースを設定することはできません。";
ORYX.I18N.SyntaxChecker.EPC_FUNCTION_AFTER_FUNCTION =  "関数の後に関数を設定することはできません。";
ORYX.I18N.SyntaxChecker.EPC_EVENT_AFTER_EVENT =  "イベントの後にイベントを設定することはできません。";
ORYX.I18N.SyntaxChecker.EPC_PI_AFTER_FUNCTION =  "関数の後にプロセスインターフェースを設定することはできません。";
ORYX.I18N.SyntaxChecker.EPC_FUNCTION_AFTER_PI =  "プロセスインターフェースの後に関数を設定することはできません。";
ORYX.I18N.SyntaxChecker.EPC_SOURCE_EQUALS_TARGET = "エッジには2つの異なるノードが接続される必要があります。"

if(!ORYX.I18N.SyntaxChecker.PetriNet) ORYX.I18N.SyntaxChecker.PetriNet={};
ORYX.I18N.SyntaxChecker.PetriNet_NOT_BIPARTITE = "2部グラフ構造は許可されません。";
ORYX.I18N.SyntaxChecker.PetriNet_NO_LABEL = "トランジションにラベルが設定されていません。";
ORYX.I18N.SyntaxChecker.PetriNet_NO_ID = "IDの無いノードがあります。";
ORYX.I18N.SyntaxChecker.PetriNet_SAME_SOURCE_AND_TARGET = "同じソースとターゲットを持つ重複したフローがあります。";
ORYX.I18N.SyntaxChecker.PetriNet_NODE_NOT_SET = "ノードがフローにセットされていません。";

/** New Language Properties: 02.06.2009*/
ORYX.I18N.Edge = "エッジ";
ORYX.I18N.Node = "ノード";

/** New Language Properties: 03.06.2009*/
ORYX.I18N.SyntaxChecker.notice = "赤い×印のアイコンにマウスカーソルを重ねてエラーメッセージを確認してください。";

/** New Language Properties: 05.06.2009*/
if(!ORYX.I18N.RESIZE) ORYX.I18N.RESIZE = {};
ORYX.I18N.RESIZE.tipGrow = "キャンパス拡大:";
ORYX.I18N.RESIZE.tipShrink = "キャンパス縮小:";
ORYX.I18N.RESIZE.N = "上";
ORYX.I18N.RESIZE.W = "左";
ORYX.I18N.RESIZE.S ="下";
ORYX.I18N.RESIZE.E ="右";

/** New Language Properties: 15.07.2009*/
if(!ORYX.I18N.Layouting) ORYX.I18N.Layouting ={};
ORYX.I18N.Layouting.doing = "レイアウト中...";

/** New Language Properties: 18.08.2009*/
ORYX.I18N.SyntaxChecker.MULT_ERRORS = "複数のエラー";

/** New Language Properties: 08.09.2009*/
if(!ORYX.I18N.PropertyWindow) ORYX.I18N.PropertyWindow = {};
ORYX.I18N.PropertyWindow.oftenUsed = "よく使う";
ORYX.I18N.PropertyWindow.moreProps = "その他のプロパティ";

/** New Language Properties 01.10.2009 */
if(!ORYX.I18N.SyntaxChecker.BPMN2) ORYX.I18N.SyntaxChecker.BPMN2 = {};

ORYX.I18N.SyntaxChecker.BPMN2_DATA_INPUT_WITH_INCOMING_DATA_ASSOCIATION = "データ入力は入力データアソシエーションを持つことができません。";
ORYX.I18N.SyntaxChecker.BPMN2_DATA_OUTPUT_WITH_OUTGOING_DATA_ASSOCIATION = "データ出力は出力データアソシエーションを持つことができません。";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_TARGET_WITH_TOO_MANY_INCOMING_SEQUENCE_FLOWS = "イベントベースゲートウェイのターゲットは入力シーケンスフローを1つだけ持つことができます。";

/** New Language Properties 02.10.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WITH_TOO_LESS_OUTGOING_SEQUENCE_FLOWS = "イベントベースゲートウェイは最低でも2つの出力シーケンスフローを持つ必要があります。";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_EVENT_TARGET_CONTRADICTION = "メッセージ中間イベントと受信タスクは同時に使うことはできません。";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WRONG_TRIGGER = "以下の中間イベントのみ利用可能です: メッセージ、シグナル、タイマー、条件、複合";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WRONG_CONDITION_EXPRESSION = "イベントゲートウェイの出力シーケンスフローには条件を設定できません。";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_NOT_INSTANTIATING = "ゲートウェイはプロセスのインスタンス化の条件を満たしていません。開始イベントもしくはインスタンス化属性を使用してください。";

/** New Language Properties 05.10.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAYDIRECTION_MIXED_FAILURE = "ゲートウェイには複数の入力/出力シーケンスフローが必要です。";
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAYDIRECTION_CONVERGING_FAILURE = "ゲートウェイには複数の入力シーケンスフローが必要ですが、出力シーケンスフローを複数設定することはできません。";
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAYDIRECTION_DIVERGING_FAILURE = "ゲートウェイには複数の出力シーケンスフローが必要ですが、入力シーケンスフローを複数設定することはできません。";
ORYX.I18N.SyntaxChecker.BPMN2_GATEWAY_WITH_NO_OUTGOING_SEQUENCE_FLOW = "ゲートウェイは最低でも1つの出力シーケンスフローを持つ必要があります。";
ORYX.I18N.SyntaxChecker.BPMN2_RECEIVE_TASK_WITH_ATTACHED_EVENT = "イベントゲートウェイの設定で使われる受信タスクには境界の中間イベントを設定できません。";
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_SUBPROCESS_BAD_CONNECTION = "イベントサブプロセスは入力/出力シーケンスフローを持つことができません。";

/** New Language Properties 13.10.2009 */
ORYX.I18N.SyntaxChecker.BPMN_MESSAGE_FLOW_NOT_CONNECTED = "メッセージフローは最低でも片側が接続されている必要があります。";

/** New Language Properties 24.11.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_TOO_MANY_INITIATING_MESSAGES = "コレオグラフィーアクティビティーは1つだけ開始メッセージを持つことができます。";
ORYX.I18N.SyntaxChecker.BPMN_MESSAGE_FLOW_NOT_ALLOWED = "ここではメッセージフローは許可されません。";

/** New Language Properties 27.11.2009 */
ORYX.I18N.SyntaxChecker.BPMN2_EVENT_BASED_WITH_TOO_LESS_INCOMING_SEQUENCE_FLOWS = "インスタンス化ではないイベントゲートウェイは最低でも1つの入力シーケンスフローを必要とします。";
ORYX.I18N.SyntaxChecker.BPMN2_TOO_FEW_INITIATING_PARTICIPANTS = "コレオグラフィーアクティビティーには開始する参加者（白）が必要です。";
ORYX.I18N.SyntaxChecker.BPMN2_TOO_MANY_INITIATING_PARTICIPANTS = "コレオグラフィーアクティビティーには開始する参加者（白）を複数設定することはできません。"

ORYX.I18N.SyntaxChecker.COMMUNICATION_AT_LEAST_TWO_PARTICIPANTS = "コミュニケーションには最低でも2つの参加者が必要です。";
ORYX.I18N.SyntaxChecker.MESSAGEFLOW_START_MUST_BE_PARTICIPANT = "メッセージフローのソースは参加者である必要があります。";
ORYX.I18N.SyntaxChecker.MESSAGEFLOW_END_MUST_BE_PARTICIPANT = "メッセージフローのターゲットは参加者である必要があります。";
ORYX.I18N.SyntaxChecker.CONV_LINK_CANNOT_CONNECT_CONV_NODES = "カンバセーションのリンクはコミュニケーションか参加者付きのサブカンバセーションノードと接続する必要があります。";
