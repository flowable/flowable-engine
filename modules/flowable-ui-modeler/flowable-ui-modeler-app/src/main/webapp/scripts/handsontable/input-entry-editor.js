(function(Handsontable){

    var MyEditor = Handsontable.editors.TextEditor.prototype.extend();

    var operatorSelectOptions = ['==', '!=', '<', '>'];

    MyEditor.prototype.createElements = function () {
        // // Call the original createElements method
        // Handsontable.editors.TextEditor.prototype.createElements.apply(this, arguments);
        //
        // this.PARENT_CONTAINER = document.createElement('div');
        // // this.PARENT_CONTAINER.className = 'input-container editor';
        //
        // this.OPERATOR_CONTAINER = document.createElement('div');
        // // this.OPERATOR_CONTAINER.className = 'input-operator-container editor';
        //
        // // Create password input and update relevant properties
        // this.OPERATOR_SELECT = document.createElement('select');
        // // this.OPERATOR_SELECT.className = 'input-operator-select';
        //
        // operatorSelectOptions.forEach(function (val) {
        //     var optionElement = document.createElement('option');
        //     optionElement.value = val;
        //     optionElement.text = val;
        //     this.OPERATOR_SELECT.appendChild(optionElement);
        // }, this);
        //
        //
        // // this.OPERATOR_SELECT.style.display = 'none';
        // // this.textareaStyle = this.TEXTAREA.style;
        // // this.textareaStyle.width = 0;
        // // this.textareaStyle.height = 0;
        //
        // this.OPERATOR_CONTAINER.appendChild(this.OPERATOR_SELECT);
        //
        // this.EXPRESSION_CONTAINER = document.createElement('div');
        // // this.EXPRESSION_CONTAINER.className = 'input-expression-container';
        //
        // // Create password input and update relevant properties
        // this.TEXTAREA2 = document.createElement('input');
        // this.TEXTAREA2.setAttribute('type', 'text');
        // // this.TEXTAREA2.className = 'handsontableInput';
        // // this.textareaStyle2 = this.TEXTAREA2.style;
        // // this.textareaStyle2.width = 0;
        // // this.textareaStyle2.height = 0;
        //
        // this.EXPRESSION_CONTAINER.appendChild(this.TEXTAREA2);
        //
        // // Replace textarea with password input
        // Handsontable.Dom.empty(this.TEXTAREA_PARENT);
        //
        // this.PARENT_CONTAINER.appendChild(this.OPERATOR_CONTAINER);
        // this.PARENT_CONTAINER.appendChild(this.EXPRESSION_CONTAINER);
        //
        //
        //
        // // Replace textarea with password input
        // Handsontable.Dom.empty(this.TEXTAREA_PARENT);
        // this.TEXTAREA_PARENT.appendChild(this.PARENT_CONTAINER);
        //
        //
        //
        //


        // Handsontable.editors.TextEditor.prototype.createElements.apply(this, arguments);
        //
        // // Create password input and update relevant properties
        // this.TEXTAREA = document.createElement('input');
        // this.TEXTAREA.setAttribute('type', 'password');
        // this.TEXTAREA.className = 'handsontableInput';
        // this.textareaStyle = this.TEXTAREA.style;
        // this.textareaStyle.width = 0;
        // this.textareaStyle.height = 0;
        //
        // Handsontable.Dom.empty(this.TEXTAREA_PARENT);
        // this.TEXTAREA_PARENT.appendChild(this.TEXTAREA);



        Handsontable.editors.TextEditor.prototype.createElements.apply(this, arguments);

        this.PARENT_CONTAINER = document.createElement('div');
        this.PARENT_CONTAINER.className = 'input-container editor';

        this.OPERATOR_CONTAINER = document.createElement('input');
        this.OPERATOR_CONTAINER.setAttribute('type', 'text');
        this.OPERATOR_CONTAINER.className = 'input-operator-container';



        this.EXPRESSION_CONTAINER = document.createElement('input');
        this.EXPRESSION_CONTAINER.setAttribute('type', 'text');
        this.EXPRESSION_CONTAINER.className = 'input-expression-container editor';


        // Create password input and update relevant properties
        this.TEXTAREA = document.createElement('div');
        // this.TEXTAREA.setAttribute('type', 'password');
        this.TEXTAREA.className = 'input-container-editor';
        this.TEXTAREA.style.width = '0';


        this.TEXTAREA.appendChild(this.OPERATOR_CONTAINER);
        this.TEXTAREA.appendChild(this.EXPRESSION_CONTAINER);


        this.PARENT_CONTAINER.appendChild(this.TEXTAREA);

        console.log(this.TEXTAREA);

        Handsontable.Dom.empty(this.TEXTAREA_PARENT);
        this.TEXTAREA_PARENT.appendChild(this.TEXTAREA);

    };


    // MyEditor.prototype.init = function () {
    //     console.log('INIT');
    //
    //     this.select = document.createElement('SELECT');
    //     Handsontable.Dom.addClass(this.select, 'htSelectEditor');
    //     this.select.style.display = 'none';
    //
    //     // Attach node to DOM, by appending it to the container holding the table
    //     this.instance.rootElement.appendChild(this.select);
    // };
    //
    // MyEditor.prototype.open = function () {
    //     console.log('OPEN');
    // };
    //
    // MyEditor.prototype.setValue = function (value) {
    //     console.log('SET VALUE');
    // };
    //
    // MyEditor.prototype.focus = function () {
    //     console.log('FOCUS');
    // };
    //
    // MyEditor.prototype.getValue = function () {
    //     console.log('GET VALUE');
    // };
    //
    // MyEditor.prototype.close = function () {
    //     console.log('CLOSE');
    // };
    //
    // MyEditor.prototype.finishEditing = function(val) {
    //     console.log('FINISHED EDITING');
    //     console.log(this);
    //     console.log(this.TD.getElementsByClassName('input-operator-select')[0].value);
    //     console.log(this.TD.getElementsByClassName('input-expression-input')[0].value);
    //
    //     var operatorValue = this.TD.getElementsByClassName('input-operator-select')[0].value;
    //     var expressionValue = this.TD.getElementsByClassName('input-expression-input')[0].value;
    //
    //     var newCellValue = {
    //         operator: operatorValue,
    //         expression: expressionValue
    //     }
    //
    //     this.instance.setDataAtCell(this.row, this.col, newCellValue, true);
    //     // console.log(this.instance.getDataAtCell(this.row, this.col));
    // };
    //
    // MyEditor.prototype.beginEditing = function() {
    //     console.log('BEGIN  EDITING');
    //
    // };
    //
    // MyEditor.prototype.saveValue = function(value) {
    //     console.log('SAVE VALUE');
    //     console.log(value);
    // };

//Put editor in dedicated namespace
    Handsontable.editors.MyEditor = MyEditor;

//Register alias
    Handsontable.editors.registerEditor('myEditor', MyEditor);

})(Handsontable);