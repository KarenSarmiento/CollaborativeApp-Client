/*
 * This file defines all JS functions that are callable from Kotlin/Android code. For example:
 *     webview.evaluateJavascript("javascript:<function-call>") {}
 *
 * We may also call Kotlin/Android functions in JS (here). This is done by using the 'ktchannel'
 * interface, which is defined in Automerge.kt.
 */

let log = function (text) { document.getElementById('output').textContent += "> " + text + "\n" }
let CHANGES = "changes"
let UPDATED_DOC = "updated_doc"

let createNewTodoList = function() {
    let newDoc = Automerge.from({ cards: [] })
    log("Created new todo list: " + newDoc)
    return Automerge.save(newDoc)
}

let mergeNewDocument = function(docToMerge) {
    // Load doc.
    let docToMergeDecoded = atob(docToMerge)

    // Merge and return
    let newDoc = Automerge.merge(Automerge.init(), Automerge.load(docToMergeDecoded))
    return Automerge.save(newDoc)
}

let getChanges = function(docPersisted, initDoc) {
    console.log(docPersisted)
    console.log(initDoc)
    // Load init doc.
    let initDecoded = atob(initDoc)
    let initLoaded = Automerge.load(initDecoded)

    // Load persisted doc.
    let docDecoded = atob(docPersisted)
    let docLoaded = Automerge.load(docDecoded)
    return Automerge.getChanges(initLoaded, docLoaded)
}

let applyJsonUpdate = function(docPersisted, changes) {
    // Load doc.
    let docDecoded = atob(docPersisted)
    let docLoaded = Automerge.load(docDecoded)
    
    // Parse changes.
    let changesDecoded = atob(changes)
    let changesParsed = JSON.parse(changesDecoded)
    // Apply changes.
    let updatedDoc = Automerge.applyChanges(docLoaded, changesParsed)
    // Return new doc.
    log("> " + JSON.stringify(updatedDoc.cards))
    ktchannel.onCardsChange(JSON.stringify(updatedDoc.cards))
    return Automerge.save(updatedDoc)
}

let applyLocalChange = function(eventName, docPersisted, changeLambda) {
    // Load doc.
    let docPersistedDecoded = atob(docPersisted)
    let doc = Automerge.load(docPersistedDecoded)

    // Apply change.
    let newDoc = Automerge.change(doc, eventName, changeLambda)
    let changes = Automerge.getChanges(doc, newDoc)

    // Return new doc.
    let newDocPersisted = Automerge.save(newDoc)
    ktchannel.onCardsChange(JSON.stringify(newDoc.cards))
    return {[CHANGES]:changes, [UPDATED_DOC]: newDocPersisted}
}

let addCard = function (docPersisted, title, completed) {
    let titleDecoded = atob(title)
    let eventName = "automerge_addCard"
    let changeLambda = it => {
            it.cards.push({ title: titleDecoded, completed:completed })
        }
    return applyLocalChange(eventName, docPersisted, changeLambda)
}

let removeCard = function (docPersisted, index) {
    let eventName = "automerge_removeCard"
    let changeLambda = it => {
            delete it.cards[index]
        }
    return applyLocalChange(eventName, docPersisted, changeLambda)
}

let setCardCompleted = function (docPersisted, index, completed) {
    let eventName = "automerge_setCardCompleted"
    let changeLambda = it => {
            it.cards[index].completed = completed
        }
    return applyLocalChange(eventName, docPersisted, changeLambda)
}
