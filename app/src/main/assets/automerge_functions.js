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

let applyJsonUpdate = function(docPersisted, changes) {
    let doc = Automerge.load(docPersisted)
    let updatedDoc = Automerge.applyChanges(doc, changes)
    log("> " + JSON.stringify(updatedDoc.cards))
    ktchannel.onCardsChange(JSON.stringify(updatedDoc.cards))
    return Automerge.save(updatedDoc)
}

let applyLocalChange = function(eventName, docPersisted, changeLambda) {
    ktchannel.startEvent(eventName)

    // Load doc.
    let docPersistedDecoded = decodeURIComponent(docPersisted)
    let doc = Automerge.load(docPersistedDecoded)

    // Apply change.
    let newDoc = Automerge.change(doc, eventName, changeLambda)
    let changes = Automerge.getChanges(doc, newDoc)

    // Return new doc.
    let newDocPersisted = Automerge.save(newDoc)

    log("> " + JSON.stringify(newDoc.cards))
    ktchannel.onCardsChange(JSON.stringify(newDoc.cards))
    ktchannel.endEvent(eventName)

    return {[CHANGES]:changes, [UPDATED_DOC]: newDocPersisted}
}

let addCard = function (docPersisted, title, completed) {
    let eventName = "automerge_addCard"
    let changeLambda = it => {
            it.cards.push({ title: title, completed:completed })
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
