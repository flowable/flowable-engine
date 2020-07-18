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
 
ORYX.I18N.PropertyWindow.dateFormat = "y/m/d";

ORYX.I18N.View.East = "属性";
ORYX.I18N.View.West = "モデリング要素";

ORYX.I18N.Oryx.title	= "Signavio";
ORYX.I18N.Oryx.pleaseWait = "Signavioプロセスエディターの起動中...";
ORYX.I18N.Edit.cutDesc = "選択部分をクリップボードへカット";
ORYX.I18N.Edit.copyDesc = "選択部分をクリップボードへコピー";
ORYX.I18N.Edit.pasteDesc = "クリップボードからキャンバスへ貼り付け";
ORYX.I18N.ERDFSupport.noCanvas = "XML文書にキャンバスノードが含まれていません。";
ORYX.I18N.ERDFSupport.noSS = "Signavioプロセスエディターのキャンバスノードにステンシルセットの定義が含まれていません。";
ORYX.I18N.ERDFSupport.deprText = "eRDFエクスポートは将来のバージョンでサポートされなくなるため推奨されません。エクスポートを実行しますか？ ";
ORYX.I18N.Save.pleaseWait = "しばらくお待ちください<br/>保存中...";

ORYX.I18N.Save.saveAs = "複製を保存...";
ORYX.I18N.Save.saveAsDesc = "複製を保存...";
ORYX.I18N.Save.saveAsTitle = "複製を保存...";
ORYX.I18N.Save.savedAs = "複製が保存されました。";
ORYX.I18N.Save.savedDescription = "プロセス図が保存されました。";
ORYX.I18N.Save.notAuthorized = "ログインしていません。 <a href='/p/login' target='_blank'>ログイン</a> してから、保存してください。"
ORYX.I18N.Save.transAborted = "保存処理に時間がかかりすぎています。より高速なインターネット接続を試してください。無線LANの場合は接続の強度を確認してください。 ";
ORYX.I18N.Save.noRights = "モデルを保存するための権限がありません。<a href='/p/explorer' target='_blank'>Signavio Explorer</a>で、保存先ディレクトリーへの書き込み権限があることを確認してください。";
ORYX.I18N.Save.comFailed = "Signavioサーバーとの通信に失敗しました。インターネット接続を確認してください。問題が解決しない場合、ツールバーの封筒アイコンからSignavioサポートへコンタクトしてください。";
ORYX.I18N.Save.failed = "保存に失敗しました。もう一度試してみてください。問題が解決しない場合、ツールバーの封筒アイコンからSignavioサポートへコンタクトしてください。";
ORYX.I18N.Save.exception = "保存時に例外が発生しました。もう一度試してみてください。問題が解決しない場合、ツールバーの封筒アイコンからSignavioサポートへコンタクトしてください。";
ORYX.I18N.Save.retrieveData = "しばらくお待ちください。データを取得中です。";

/** New Language Properties: 10.6.09*/
if(!ORYX.I18N.ShapeMenuPlugin) ORYX.I18N.ShapeMenuPlugin = {};
ORYX.I18N.ShapeMenuPlugin.morphMsg = "図形を変形";
ORYX.I18N.ShapeMenuPlugin.morphWarningTitleMsg = "図形を変形";
ORYX.I18N.ShapeMenuPlugin.morphWarningMsg = "変形した要素には所属できない子図形があります。<br/>変形を実行しますか？";

if (!Signavio) { var Signavio = {}; }
if (!Signavio.I18N) { Signavio.I18N = {} }
if (!Signavio.I18N.Editor) { Signavio.I18N.Editor = {} }

if (!Signavio.I18N.Editor.Linking) { Signavio.I18N.Editor.Linking = {} }
Signavio.I18N.Editor.Linking.CreateDiagram = "新規作成";
Signavio.I18N.Editor.Linking.UseDiagram = "既存のダイアグラムを使用";
Signavio.I18N.Editor.Linking.UseLink = "Webリンクを使用";
Signavio.I18N.Editor.Linking.Close = "閉じる";
Signavio.I18N.Editor.Linking.Cancel = "キャンセル";
Signavio.I18N.Editor.Linking.UseName = "名前をあわせる";
Signavio.I18N.Editor.Linking.UseNameHint = "モデリング要素({type})の名前をリンクしたダイアグラムの名前で置き換える。";
Signavio.I18N.Editor.Linking.CreateTitle = "リンクを作成";
Signavio.I18N.Editor.Linking.AlertSelectModel = "モデルを選ぶ必要があります。";
Signavio.I18N.Editor.Linking.ButtonLink = "ダイアグラムをリンク";
Signavio.I18N.Editor.Linking.LinkNoAccess = "このダイアグラムへの権限がありません。";
Signavio.I18N.Editor.Linking.LinkUnavailable = "ダイアグラムが利用不能です。";
Signavio.I18N.Editor.Linking.RemoveLink = "リンクを削除";
Signavio.I18N.Editor.Linking.EditLink = "リンクを編集";
Signavio.I18N.Editor.Linking.OpenLink = "開く";
Signavio.I18N.Editor.Linking.BrokenLink = "リンクが壊れています。";
Signavio.I18N.Editor.Linking.PreviewTitle = "プレビュー";

if(!Signavio.I18N.Glossary_Support) { Signavio.I18N.Glossary_Support = {}; }
Signavio.I18N.Glossary_Support.renameEmpty = "辞書エントリーがありません";
Signavio.I18N.Glossary_Support.renameLoading = "検索中...";

/** New Language Properties: 08.09.2009*/
if(!ORYX.I18N.PropertyWindow) ORYX.I18N.PropertyWindow = {};
ORYX.I18N.PropertyWindow.oftenUsed = "主な属性";
ORYX.I18N.PropertyWindow.moreProps = "その他の属性";

ORYX.I18N.PropertyWindow.btnOpen = "開く";
ORYX.I18N.PropertyWindow.btnRemove = "削除";
ORYX.I18N.PropertyWindow.btnEdit = "編集";
ORYX.I18N.PropertyWindow.btnUp = "上へ移動";
ORYX.I18N.PropertyWindow.btnDown = "下へ移動";
ORYX.I18N.PropertyWindow.createNew = "新規作成";

if(!ORYX.I18N.PropertyWindow) ORYX.I18N.PropertyWindow = {};
ORYX.I18N.PropertyWindow.oftenUsed = "主な属性";
ORYX.I18N.PropertyWindow.moreProps = "その他の属性";
ORYX.I18N.PropertyWindow.characteristicNr = "コスト &amp; リソース分析";
ORYX.I18N.PropertyWindow.meta = "カスタム属性";

if(!ORYX.I18N.PropertyWindow.Category){ORYX.I18N.PropertyWindow.Category = {}}
ORYX.I18N.PropertyWindow.Category.popular = "主な属性";
ORYX.I18N.PropertyWindow.Category.characteristicnr = "コスト &amp; リソース分析";
ORYX.I18N.PropertyWindow.Category.others = "その他の属性";
ORYX.I18N.PropertyWindow.Category.meta = "カスタム属性";

if(!ORYX.I18N.PropertyWindow.ListView) ORYX.I18N.PropertyWindow.ListView = {};
ORYX.I18N.PropertyWindow.ListView.title = "編集: ";
ORYX.I18N.PropertyWindow.ListView.dataViewLabel = "すでにエントリーが存在します。";
ORYX.I18N.PropertyWindow.ListView.dataViewEmptyText = "エントリーがありません。";
ORYX.I18N.PropertyWindow.ListView.addEntryLabel = "新しいエントリーの追加";
ORYX.I18N.PropertyWindow.ListView.buttonAdd = "追加";
ORYX.I18N.PropertyWindow.ListView.save = "保存";
ORYX.I18N.PropertyWindow.ListView.cancel = "キャンセル";

if(!Signavio.I18N.Buttons) Signavio.I18N.Buttons = {};
Signavio.I18N.Buttons.save		= "保存";
Signavio.I18N.Buttons.cancel 	= "キャンセル";
Signavio.I18N.Buttons.remove	= "削除";

if(!Signavio.I18N.btn) {Signavio.I18N.btn = {};}
Signavio.I18N.btn.btnEdit = "編集";
Signavio.I18N.btn.btnRemove = "削除";
Signavio.I18N.btn.moveUp = "上へ移動";
Signavio.I18N.btn.moveDown = "下へ移動";

if(!Signavio.I18N.field) {Signavio.I18N.field = {};}
Signavio.I18N.field.Url = "URL";
Signavio.I18N.field.UrlLabel = "ラベル";
