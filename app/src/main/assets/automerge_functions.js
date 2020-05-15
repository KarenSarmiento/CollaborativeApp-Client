/*
 * This file defines all JS functions that are callable from Kotlin/Android code. For example:
 *     webview.evaluateJavascript("javascript:<function-call>") {}
 *
 * We may also call Kotlin/Android functions in JS (here). This is done by using the 'ktchannel'
 * interface, which is defined in Automerge.kt.
 *
 * It can be assumed that functions are atomically run (thanks to Kotlin synchronisation).
 *
 */

// TODO: Pass group names into each function, instead of just using "grp" for everything.
let CHANGES = "changes"
let UPDATED_DOC = "updated_doc"
let map = new Map()


// If not in map, decode document and save json to map.
// If in map, get json from map.
let getDoc = function(group_name, document) {
    if (map.has(group_name))
        return map.get(group_name)
    let docDecoded = atob(document)
    let docLoaded = Automerge.load(docDecoded)
    map.set(group_name, docLoaded)
    return docLoaded
}

let createNewTodoList = function() {
    let newDoc = Automerge.from({ cards: [] })
    map.set("grp", newDoc)
    return Automerge.save(newDoc)
}

let mergeNewDocument = function(docToMerge) {
    // Load doc.
    let doc = getDoc("grp", docToMerge)

    // Merge and return
    let newDoc = Automerge.merge(Automerge.init(), doc)
    map.set("grp", newDoc)
    ktchannel.onCardsChange(JSON.stringify(newDoc.cards))
    return Automerge.save(newDoc)
}

let applyJsonUpdate = function(docPersisted, changes) {
    // Load doc.
    let docLoaded = getDoc("grp", docPersisted)

    // Parse changes.
    let changesDecoded = atob(changes)
    let changesParsed = JSON.parse(changesDecoded)
    // Apply changes.
    let updatedDoc = Automerge.applyChanges(docLoaded, changesParsed)
    map.set("grp", updatedDoc)
    // Return new doc.
    ktchannel.onCardsChange(JSON.stringify(updatedDoc.cards))
    return Automerge.save(updatedDoc)
}

let applyLocalChange = function(eventName, docPersisted, changeLambda) {
    // Load doc.
    let doc = getDoc("grp", docPersisted)

    // Apply change.
    let newDoc = Automerge.change(doc, eventName, changeLambda)
    let changes = Automerge.getChanges(doc, newDoc)
    map.set("grp", newDoc)

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

let updateCardsUI = function(docPersisted) {
    let doc = getDoc("grp", docPersisted)
    ktchannel.onCardsChange(JSON.stringify(doc.cards))
}

let resetDoc = function (document) {
    let docDecoded = atob(document)
    let docLoaded = Automerge.load(docDecoded)
    map.set("grp", docLoaded)
}