Ky projekt paraqet një aplikacion Server–Client të zhvilluar në Java, i cili mundëson komunikimin në kohë reale midis disa klientëve dhe një serveri qendror. 
Qëllimi i sistemit është të ofrojë funksione të menaxhimit të skedarëve nëpërmjet rrjetit, si: listimi, leximi, kërkimi, shkarkimi, ngarkimi dhe fshirja e file-ve.
Projekti mbështet dy nivele përdoruesish: admin dhe përdorues normal, të cilët kanë qasje të ndryshme në komanda.

Serveri pranon deri në një numër të kufizuar klientësh njëkohësisht duke përdorur një thread pool, dhe çdo klient menaxhohet në një fije të veçantë. 
Çdo lidhje ka një timeout automatik prej 5 minutash nëse klienti nuk dërgon asnjë mesazh. Serveri gjithashtu ruan statistika për aktivitetin e klientëve, si numri i mesazheve, trafikimi i bytes, dhe lista e lidhjeve aktive. 
Të gjitha veprimet dhe mesazhet e klientëve regjistrohen në file log.

Pasi klienti lidhet, ai kryen identifikimin. Nëse jep kredencialet "admin:password", merr rolin e administratorit; në të kundërt, klasifikohet si përdorues i thjeshtë.
Përdoruesi normal ka të drejtë të lexojë dhe kërkojë skedarë, ndërsa admini ka kontrolle të plota mbi sistemin e file-ve, përfshirë ngarkimin dhe fshirjen e dokumenteve.

Administratori ka qasje të plotë mbi serverin. Ai mund të:

- Listojë të gjithë skedarët në server.

- Lexojë përmbajtjen e çfarëdo skedari.

- Kontrollojë informata për një skedar, si madhësia dhe data e modifikimit.

- Kërkojë për tekst brenda të gjithë skedarëve.

- Shkarkojë skedarë lokalisht.

- Ngarkojë skedarë të rinj në server.

- Fshijë skedarë nga serveri.

- Shikojë statistikat e serverit në çdo moment.

Admini shërben si operatori kryesor që menaxhon skedarët në server dhe miremban sistemin.

Përdoruesi normal ka qasje të kufizuar. Ai mund të:

- Dërgojë komanda të sigurta që nuk modifikojnë sistemin.

- Listojë skedarët e disponueshëm në server.

- Lexojë skedarë ekzistues.

- Kontrollojë informacionet për një skedar.

- Kërkojë për fjali brenda të gjithë skedarëve.

- Shkarkojë skedarë nga serveri.

- Shikojë statistikat e serverit.

Përdoruesi normal nuk mund të ngarkojë ose fshijë skedarë, për arsye sigurie.

Punuan: Albena Mehmeti, Arisa Dragusha, Kaltrinë Heta
