/*
 * This file defines all JS functions that are callable from Kotlin/Android code. For example:
 *     webview.evaluateJavascript("javascript:<function-call>") {}
 *
 * We may also call Kotlin/Android functions in JS (here). This is done by using the 'ktchannel'
 * interface, which is defined in Automerge.kt.
 */

let log = function (text) { document.getElementById('output').textContent += "> " + text + "\n" }

let doc = Automerge.from({ cards: [] })

let addCard = function (title) {
    event_name = "automerge_addCard"
    ktchannel.startEvent(event_name)

    doc = Automerge.change(doc, 'Add card', it => {
        it.cards.push({ title: title })
    })
    log("> " + Automerge.save(doc))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(event_name)
}

let removeCard = function () {
    event_name = "automerge_removeCard"
    ktchannel.startEvent(event_name)

    doc = Automerge.change(doc, 'Delete card', it => {
      delete it.cards[0]
    })
    log("> " + Automerge.save(doc))
    ktchannel.onCardsChange(JSON.stringify(doc.cards))

    ktchannel.endEvent(event_name)
}