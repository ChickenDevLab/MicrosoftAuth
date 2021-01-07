# Websocket Docs

Alle Nachrichten werden als JSON gesendet:
```JSON
{
    "code": "code",
    "payload1": "payload1val",
    "payload2": "payload2val"
}
```
---
## Serverseitig

Code | Payloads | Bedeutung
---- | ------- | ---------
`nocode` | null | Wird gesendet, wenn eine Nachricht von einem Client keinen Code enthält
`waitforstate` | null | Server wartet auf `state`
`stateaccept` | null | Wird gesendet, wenn der mit `state` gesendete Status-Code aktzeptiert wird
`statedeny` | null | Wird gesendet, wenn der Status-Code ungültig ist (z. B. bereits in Verwendung)
`alstaterec` | null | Session hat schon einen Code gesendet, der aktzeptiert wurde
`authreceiv` | null | Wird gesendet, wenn auf `redirect` ein Authorisation-Code ankommt
`final` | `response` - Enthält `refresh_token`, `access_token`, etc. | Sendet die finale Antwort mit allen benötigten Daten
`authdeny` | null | Authorisierung fehlgeschlagen

## Clientseitig

Code | Payloads | Bedeutung
---- | ------- | ---------
`state` | `state` - State | Enthält den zufällig generierten Status-Code