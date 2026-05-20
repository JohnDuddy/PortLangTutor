from __future__ import annotations

import json
import re
import unicodedata
from itertools import product
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "phrases_v1.json"

TARGETS = {
    "survival": 110,
    "travel": 110,
    "restaurants": 120,
    "conversation": 120,
    "dating_social": 90,
    "medical": 110,
    "work": 105,
    "shopping": 105,
    "transportation": 110,
    "emotions": 90,
    "questions": 110,
    "verbs": 160,
    "sentence_patterns": 160,
}


def slot(pt: str, en: str, tag: str | None = None) -> dict[str, str]:
    fallback_tag = re.sub(r"[^a-z0-9]+", "-", en.lower()).strip("-")
    return {"pt": pt, "en": en, "tag": tag or fallback_tag}


NEEDS = [
    slot("água", "water"), slot("ajuda", "help"), slot("um carregador", "a charger"),
    slot("internet", "internet"), slot("um banheiro", "a bathroom"), slot("um médico", "a doctor"),
    slot("um táxi", "a taxi"), slot("comida", "food"), slot("um mapa", "a map"),
    slot("um recibo", "a receipt"), slot("troco", "change"), slot("um telefone", "a phone"),
    slot("um tradutor", "a translator"), slot("uma farmácia", "a pharmacy"),
    slot("um caixa eletrônico", "an ATM"), slot("um lugar seguro", "a safe place"),
    slot("uma tomada", "an outlet"), slot("um documento", "a document"),
    slot("meu passaporte", "my passport"), slot("meu cartão", "my card"),
    slot("dinheiro", "cash"), slot("uma informação", "some information"),
    slot("um endereço", "an address"), slot("uma chave", "a key"),
]

PLACES = [
    slot("o banheiro", "the bathroom"), slot("a saída", "the exit"), slot("a farmácia", "the pharmacy"),
    slot("o hospital", "the hospital"), slot("a delegacia", "the police station"),
    slot("o ponto de táxi", "the taxi stand"), slot("a estação", "the station"),
    slot("o aeroporto", "the airport"), slot("o hotel", "the hotel"),
    slot("o caixa eletrônico", "the ATM"), slot("a recepção", "the front desk"),
    slot("a bilheteria", "the ticket office"), slot("o portão", "the gate"),
    slot("a plataforma", "the platform"), slot("a loja", "the store"),
    slot("o restaurante", "the restaurant"), slot("a praia", "the beach"),
    slot("o centro", "downtown"), slot("a rua principal", "the main street"),
    slot("o consulado", "the consulate"),
]

PROBLEMS = [
    slot("meu celular", "my phone"), slot("meu passaporte", "my passport"),
    slot("minha mala", "my suitcase"), slot("meu cartão", "my card"),
    slot("a reserva", "the reservation"), slot("o endereço", "the address"),
    slot("a senha", "the password"), slot("o aplicativo", "the app"),
    slot("a passagem", "the ticket"), slot("a chave", "the key"),
    slot("o pagamento", "the payment"), slot("a conta", "the bill"),
    slot("a internet", "the internet"), slot("o quarto", "the room"),
    slot("o carro", "the car"), slot("o pedido", "the order"),
]

PEOPLE = [
    slot("um funcionário", "an employee"), slot("a recepção", "the front desk"),
    slot("um policial", "a police officer"), slot("um médico", "a doctor"),
    slot("um gerente", "a manager"), slot("alguém que fale inglês", "someone who speaks English"),
    slot("o motorista", "the driver"), slot("o atendente", "the attendant"),
]

TRAVEL_DOCS = [
    slot("meu passaporte", "my passport"), slot("meu visto", "my visa"),
    slot("minha passagem", "my ticket"), slot("meu cartão de embarque", "my boarding pass"),
    slot("minha reserva", "my reservation"), slot("meu documento", "my ID"),
    slot("o comprovante", "the proof"), slot("o endereço do hotel", "the hotel address"),
]

TRAVEL_RESERVATIONS = [
    slot("um voo", "a flight"), slot("um hotel", "a hotel"),
    slot("um quarto", "a room"), slot("um passeio", "a tour"),
    slot("um carro", "a car"), slot("uma mesa", "a table"),
    slot("uma passagem", "a ticket"), slot("um traslado", "a transfer"),
]

TIMES = [
    slot("hoje", "today"), slot("amanhã", "tomorrow"), slot("mais tarde", "later"),
    slot("esta manhã", "this morning"), slot("esta tarde", "this afternoon"),
    slot("esta noite", "tonight"), slot("na sexta-feira", "on Friday"),
    slot("no fim de semana", "on the weekend"), slot("às oito", "at eight"),
    slot("ao meio-dia", "at noon"), slot("em uma hora", "in one hour"),
    slot("daqui a pouco", "soon"),
]

FOODS = [
    slot("arroz", "rice"), slot("feijão", "beans"), slot("frango", "chicken"),
    slot("peixe", "fish"), slot("carne", "meat"), slot("salada", "salad"),
    slot("sopa", "soup"), slot("pão de queijo", "cheese bread"),
    slot("café", "coffee"), slot("café com leite", "coffee with milk"),
    slot("água sem gás", "still water"), slot("água com gás", "sparkling water"),
    slot("suco de laranja", "orange juice"), slot("cerveja", "beer"),
    slot("vinho", "wine"), slot("sobremesa", "dessert"), slot("a conta", "the check"),
    slot("o cardápio", "the menu"), slot("uma mesa", "a table"), slot("um prato vegetariano", "a vegetarian dish"),
]

INGREDIENTS = [
    slot("açúcar", "sugar"), slot("sal", "salt"), slot("leite", "milk"), slot("carne", "meat"),
    slot("queijo", "cheese"), slot("glúten", "gluten"), slot("pimenta", "pepper"),
    slot("cebola", "onion"), slot("alho", "garlic"), slot("molho", "sauce"),
    slot("gelo", "ice"), slot("maionese", "mayonnaise"),
]

TOPICS = [
    slot("música brasileira", "Brazilian music"), slot("futebol", "soccer"),
    slot("comida brasileira", "Brazilian food"), slot("praias", "beaches"),
    slot("cinema", "movies"), slot("livros", "books"), slot("café", "coffee"),
    slot("viagens", "travel"), slot("tecnologia", "technology"), slot("trabalho", "work"),
    slot("família", "family"), slot("história", "history"), slot("arte", "art"),
    slot("natureza", "nature"), slot("carros", "cars"), slot("animais", "animals"),
    slot("exercícios", "exercise"), slot("fotografia", "photography"),
]

ACTIVITIES = [
    slot("tomar um café", "have coffee"), slot("jantar", "have dinner"),
    slot("dar uma volta", "take a walk"), slot("ir ao cinema", "go to the movies"),
    slot("ver música ao vivo", "see live music"), slot("conhecer o centro", "see downtown"),
    slot("ir à praia", "go to the beach"), slot("conversar mais", "talk more"),
    slot("sair hoje", "go out today"), slot("marcar outro dia", "plan another day"),
]

SYMPTOMS = [
    slot("febre", "a fever"), slot("dor de cabeça", "a headache"),
    slot("dor de garganta", "a sore throat"), slot("dor no peito", "chest pain"),
    slot("dor nas costas", "back pain"), slot("náusea", "nausea"),
    slot("tontura", "dizziness"), slot("tosse", "a cough"),
    slot("falta de ar", "shortness of breath"), slot("alergia", "an allergy"),
    slot("dor de estômago", "a stomachache"), slot("cansaço", "fatigue"),
    slot("ansiedade", "anxiety"), slot("um corte", "a cut"), slot("uma queimadura", "a burn"),
]

BODY_PARTS = [
    slot("a cabeça", "my head"), slot("a garganta", "my throat"), slot("o peito", "my chest"),
    slot("as costas", "my back"), slot("o braço", "my arm"), slot("a perna", "my leg"),
    slot("o joelho", "my knee"), slot("o pé", "my foot"), slot("a mão", "my hand"),
    slot("o estômago", "my stomach"), slot("o ouvido", "my ear"), slot("o dente", "my tooth"),
]

WORK_ITEMS = [
    slot("o relatório", "the report"), slot("a apresentação", "the presentation"),
    slot("a reunião", "the meeting"), slot("o orçamento", "the budget"),
    slot("o contrato", "the contract"), slot("o e-mail", "the email"),
    slot("o prazo", "the deadline"), slot("a planilha", "the spreadsheet"),
    slot("o cliente", "the client"), slot("o projeto", "the project"),
    slot("a proposta", "the proposal"), slot("o arquivo", "the file"),
]

SHOPPING_ITEMS = [
    slot("esta camisa", "this shirt"), slot("este sapato", "this shoe"),
    slot("esta calça", "these pants"), slot("esta bolsa", "this bag"),
    slot("este presente", "this gift"), slot("este carregador", "this charger"),
    slot("este remédio", "this medicine"), slot("este produto", "this product"),
    slot("esta lembrança", "this souvenir"), slot("este tamanho", "this size"),
    slot("esta cor", "this color"), slot("outro modelo", "another model"),
    slot("um pacote para presente", "gift wrapping"),
]

TRANSPORTS = [
    slot("o ônibus", "the bus"), slot("o metrô", "the subway"),
    slot("o trem", "the train"), slot("um táxi", "a taxi"),
    slot("um carro por aplicativo", "a rideshare car"), slot("a balsa", "the ferry"),
    slot("o bonde", "the tram"), slot("o avião", "the plane"),
]

DESTINATIONS = [
    slot("o aeroporto", "the airport"), slot("o hotel", "the hotel"),
    slot("o centro", "downtown"), slot("a praia", "the beach"),
    slot("a estação", "the station"), slot("o hospital", "the hospital"),
    slot("o restaurante", "the restaurant"), slot("a reunião", "the meeting"),
    slot("o museu", "the museum"), slot("a rodoviária", "the bus station"),
    slot("a universidade", "the university"), slot("o shopping", "the mall"),
]

EMOTIONS = [
    slot("feliz", "happy"), slot("animado", "excited"), slot("cansado", "tired"),
    slot("preocupado", "worried"), slot("confuso", "confused"), slot("tranquilo", "calm"),
    slot("nervoso", "nervous"), slot("orgulhoso", "proud"), slot("grato", "grateful"),
    slot("chateado", "upset"), slot("com medo", "afraid"), slot("com saudade", "missing someone"),
    slot("com fome", "hungry"), slot("com sede", "thirsty"), slot("ocupado", "busy"),
    slot("aliviado", "relieved"), slot("surpreso", "surprised"), slot("esperançoso", "hopeful"),
]

QUESTION_ACTIONS = [
    slot("pagar", "pay"), slot("entrar", "enter"), slot("sair", "leave"),
    slot("comprar isso", "buy this"), slot("trocar dinheiro", "exchange money"),
    slot("pegar um táxi", "get a taxi"), slot("fazer check-in", "check in"),
    slot("reservar uma mesa", "reserve a table"), slot("usar o banheiro", "use the bathroom"),
    slot("carregar o celular", "charge my phone"), slot("falar com alguém", "talk to someone"),
    slot("confirmar a reserva", "confirm the reservation"), slot("mudar o horário", "change the time"),
    slot("pedir a conta", "ask for the check"), slot("encontrar o endereço", "find the address"),
    slot("comprar uma passagem", "buy a ticket"), slot("marcar uma consulta", "schedule an appointment"),
    slot("enviar uma mensagem", "send a message"), slot("entrar na fila", "get in line"),
    slot("pegar minha mala", "pick up my suitcase"), slot("fazer uma pergunta", "ask a question"),
]

VERB_ACTIONS = [
    slot("falar português", "speak Portuguese"), slot("entender melhor", "understand better"),
    slot("pedir ajuda", "ask for help"), slot("comprar isso", "buy this"),
    slot("pagar com cartão", "pay by card"), slot("chamar um táxi", "call a taxi"),
    slot("reservar um quarto", "book a room"), slot("marcar uma reunião", "schedule a meeting"),
    slot("enviar o arquivo", "send the file"), slot("aprender mais", "learn more"),
    slot("praticar hoje", "practice today"), slot("voltar amanhã", "come back tomorrow"),
    slot("esperar aqui", "wait here"), slot("ir embora", "leave"), slot("chegar cedo", "arrive early"),
    slot("resolver o problema", "solve the problem"),
    slot("fazer uma pergunta", "ask a question"), slot("pedir uma recomendação", "ask for a recommendation"),
    slot("confirmar o horário", "confirm the time"), slot("mudar a reserva", "change the reservation"),
    slot("guardar minha mala", "store my suitcase"), slot("buscar meu pedido", "pick up my order"),
    slot("ligar para o hotel", "call the hotel"), slot("encontrar meus amigos", "meet my friends"),
    slot("usar o aplicativo", "use the app"), slot("seguir as instruções", "follow the instructions"),
]


FRAMESETS = {
    "survival": [
        ("needs", "Eu preciso de {x.pt}.", "I need {x.en}.", {"x": NEEDS}, "A1", "Eu preciso de + noun"),
        ("location", "Onde fica {x.pt}?", "Where is {x.en}?", {"x": PLACES}, "A1", "Onde fica + place"),
        ("problem", "Pode me ajudar com {x.pt}?", "Can you help me with {x.en}?", {"x": PROBLEMS}, "A2", "Pode + infinitive"),
        ("lost", "Eu perdi {x.pt}.", "I lost {x.en}.", {"x": TRAVEL_DOCS + PROBLEMS[:6]}, "A1", "Eu perdi + object"),
        ("urgent", "É urgente: preciso de {x.pt}.", "It is urgent: I need {x.en}.", {"x": NEEDS[:18]}, "A2", "É urgente + clause"),
        ("cannot_find", "Não encontro {x.pt}.", "I cannot find {x.en}.", {"x": PLACES + PROBLEMS[:8]}, "A2", "Não encontro + noun"),
        ("speak_to", "Preciso falar com {x.pt}.", "I need to speak with {x.en}.", {"x": PEOPLE}, "A2", "Falar com + person"),
    ],
    "travel": [
        ("documents", "Preciso mostrar {x.pt}.", "I need to show {x.en}.", {"x": TRAVEL_DOCS}, "A1", "Preciso + infinitive"),
        ("timing", "Minha viagem é {x.pt}.", "My trip is {x.en}.", {"x": TIMES}, "A1", "Ser for schedules"),
        ("hotel", "Tenho uma reserva para {x.pt}.", "I have a reservation for {x.en}.", {"x": TIMES}, "A1", "Tenho uma reserva"),
        ("airport", "Onde fica {x.pt} no aeroporto?", "Where is {x.en} in the airport?", {"x": PLACES[10:15] + TRAVEL_DOCS[:4]}, "A2", "Onde fica"),
        ("plans", "Vou visitar {x.pt}.", "I am going to visit {x.en}.", {"x": DESTINATIONS}, "A1", "Vou + infinitive"),
        ("stay", "Vou ficar aqui até {x.pt}.", "I will stay here until {x.en}.", {"x": TIMES}, "A2", "Até + time"),
        ("reserve", "Quero reservar {x.pt}.", "I want to book {x.en}.", {"x": TRAVEL_RESERVATIONS}, "A1", "Quero + infinitive"),
        ("duration", "Quanto tempo leva até {x.pt}?", "How long does it take to get to {x.en}?", {"x": DESTINATIONS}, "A2", "Quanto tempo leva...?"),
        ("departure", "Meu voo para {x.pt} sai {y.pt}.", "My flight to {x.en} leaves {y.en}.", {"x": DESTINATIONS[:8], "y": TIMES[:8]}, "B1", "Sair + time"),
    ],
    "restaurants": [
        ("ordering", "Gostaria de {x.pt}, por favor.", "I would like {x.en}, please.", {"x": FOODS}, "A1", "Gostaria de + noun"),
        ("without", "Sem {x.pt}, por favor.", "Without {x.en}, please.", {"x": INGREDIENTS}, "A1", "Sem + noun"),
        ("with", "Com {x.pt}, por favor.", "With {x.en}, please.", {"x": INGREDIENTS}, "A1", "Com + noun"),
        ("questions", "Tem {x.pt}?", "Do you have {x.en}?", {"x": FOODS + INGREDIENTS}, "A1", "Tem + noun?"),
        ("service", "Pode trazer {x.pt}?", "Can you bring {x.en}?", {"x": FOODS[:12] + [slot("a conta", "the check"), slot("mais guardanapos", "more napkins")]}, "A2", "Pode trazer + noun"),
        ("opinion", "{x.pt} está muito bom.", "{x.en} is very good.", {"x": FOODS[:16]}, "A1", "Está + adjective"),
        ("recommend", "Você recomenda {x.pt}?", "Do you recommend {x.en}?", {"x": FOODS[:16]}, "A2", "Recomendar + noun"),
        ("allergy", "Tenho alergia a {x.pt}.", "I am allergic to {x.en}.", {"x": INGREDIENTS}, "A2", "Ter alergia a"),
    ],
    "conversation": [
        ("likes", "Eu gosto de {x.pt}.", "I like {x.en}.", {"x": TOPICS}, "A1", "Gostar de"),
        ("ask_likes", "Você gosta de {x.pt}?", "Do you like {x.en}?", {"x": TOPICS}, "A1", "Você gosta de...?"),
        ("opinion", "O que você acha de {x.pt}?", "What do you think of {x.en}?", {"x": TOPICS}, "A2", "O que você acha de...?"),
        ("experience", "Estou aprendendo sobre {x.pt}.", "I am learning about {x.en}.", {"x": TOPICS}, "A2", "Estou + gerund"),
        ("weekend", "No fim de semana, quero {x.pt}.", "On the weekend, I want to {x.en}.", {"x": ACTIVITIES}, "A2", "Quero + infinitive"),
        ("followup", "Me fala mais sobre {x.pt}.", "Tell me more about {x.en}.", {"x": TOPICS}, "A2", "Falar sobre"),
        ("learning", "Quero aprender mais sobre {x.pt}.", "I want to learn more about {x.en}.", {"x": TOPICS}, "A2", "Aprender sobre"),
        ("story", "Tenho uma história sobre {x.pt}.", "I have a story about {x.en}.", {"x": TOPICS}, "B1", "Ter uma história sobre"),
    ],
    "dating_social": [
        ("invite", "Quer {x.pt} comigo?", "Do you want to {x.en} with me?", {"x": ACTIVITIES}, "A2", "Querer + infinitive"),
        ("plans", "Vamos {x.pt} {y.pt}?", "Shall we {x.en} {y.en}?", {"x": ACTIVITIES[:8], "y": TIMES[:8]}, "A2", "Vamos + infinitive"),
        ("compliment", "Você tem um sorriso muito bonito.", "You have a very beautiful smile.", {}, "A2", "Ter + noun"),
        ("message", "Posso te mandar uma mensagem?", "Can I send you a message?", {}, "A2", "Posso + infinitive"),
        ("boundary", "Prefiro ir devagar.", "I prefer to go slowly.", {}, "A2", "Preferir + infinitive"),
        ("connection", "Gostei muito da nossa conversa.", "I really liked our conversation.", {}, "B1", "Gostar de + noun"),
        ("availability", "Você está livre {x.pt}?", "Are you free {x.en}?", {"x": TIMES}, "A2", "Estar livre"),
        ("place", "Quer se encontrar em {x.pt}?", "Do you want to meet at {x.en}?", {"x": DESTINATIONS[:8]}, "A2", "Encontrar-se em"),
    ],
    "medical": [
        ("symptoms", "Estou com {x.pt}.", "I have {x.en}.", {"x": SYMPTOMS}, "A1", "Estou com + symptom"),
        ("pain", "Estou com dor em {x.pt}.", "I have pain in {x.en}.", {"x": BODY_PARTS}, "A1", "Dor em + body part"),
        ("need", "Preciso de {x.pt}.", "I need {x.en}.", {"x": [slot("um médico", "a doctor"), slot("uma enfermeira", "a nurse"), slot("um remédio", "medicine"), slot("uma farmácia", "a pharmacy"), slot("um hospital", "a hospital"), slot("uma receita", "a prescription"), slot("um exame", "a test"), slot("atendimento urgente", "urgent care")]}, "A1", "Preciso de + noun"),
        ("allergy", "Sou alérgico a {x.pt}.", "I am allergic to {x.en}.", {"x": INGREDIENTS[:8]}, "A2", "Ser alérgico a"),
        ("duration", "Estou assim desde {x.pt}.", "I have been like this since {x.en}.", {"x": TIMES}, "A2", "Desde + time"),
        ("care", "Posso tomar {x.pt}?", "Can I take {x.en}?", {"x": [slot("este remédio", "this medicine"), slot("água", "water"), slot("um analgésico", "a pain reliever"), slot("um antibiótico", "an antibiotic")]}, "A2", "Tomar remédio"),
        ("symptom_time", "Estou com {x.pt} desde {y.pt}.", "I have had {x.en} since {y.en}.", {"x": SYMPTOMS[:10], "y": TIMES[:8]}, "B1", "Desde + time"),
        ("appointment", "Preciso marcar uma consulta {x.pt}.", "I need to schedule an appointment {x.en}.", {"x": TIMES}, "A2", "Marcar uma consulta"),
        ("severity", "A dor em {x.pt} está forte.", "The pain in {x.en} is strong.", {"x": BODY_PARTS}, "A2", "Dor em + body part"),
    ],
    "work": [
        ("need", "Preciso terminar {x.pt}.", "I need to finish {x.en}.", {"x": WORK_ITEMS}, "A2", "Preciso + infinitive"),
        ("review", "Você pode revisar {x.pt}?", "Can you review {x.en}?", {"x": WORK_ITEMS}, "A2", "Pode + infinitive"),
        ("send", "Vou enviar {x.pt} {y.pt}.", "I will send {x.en} {y.en}.", {"x": WORK_ITEMS, "y": TIMES[:8]}, "A2", "Vou + infinitive"),
        ("meeting", "Tenho uma reunião {x.pt}.", "I have a meeting {x.en}.", {"x": TIMES}, "A1", "Tenho + noun"),
        ("status", "{x.pt} está pronto.", "{x.en} is ready.", {"x": WORK_ITEMS}, "A2", "Está + adjective"),
    ],
    "shopping": [
        ("buy", "Quero comprar {x.pt}.", "I want to buy {x.en}.", {"x": SHOPPING_ITEMS}, "A1", "Quero + infinitive"),
        ("price", "Quanto custa {x.pt}?", "How much does {x.en} cost?", {"x": SHOPPING_ITEMS}, "A1", "Quanto custa...?"),
        ("have", "Tem {x.pt}?", "Do you have {x.en}?", {"x": SHOPPING_ITEMS}, "A1", "Tem...?"),
        ("try", "Posso experimentar {x.pt}?", "Can I try on {x.en}?", {"x": SHOPPING_ITEMS[:5]}, "A2", "Posso + infinitive"),
        ("return", "Posso trocar {x.pt}?", "Can I exchange {x.en}?", {"x": SHOPPING_ITEMS}, "A2", "Trocar + noun"),
        ("preference", "Prefiro {x.pt}.", "I prefer {x.en}.", {"x": SHOPPING_ITEMS[8:] + [slot("algo mais barato", "something cheaper"), slot("outra cor", "another color"), slot("outro tamanho", "another size")]}, "A2", "Preferir + noun"),
        ("size", "Você tem {x.pt} em outro tamanho?", "Do you have {x.en} in another size?", {"x": SHOPPING_ITEMS[:10]}, "A2", "Ter em outro tamanho"),
        ("color", "Você tem {x.pt} em outra cor?", "Do you have {x.en} in another color?", {"x": SHOPPING_ITEMS[:10]}, "A2", "Ter em outra cor"),
        ("receipt", "Preciso do recibo de {x.pt}.", "I need the receipt for {x.en}.", {"x": SHOPPING_ITEMS}, "A2", "Recibo de + noun"),
        ("discount", "Tem desconto para {x.pt}?", "Is there a discount for {x.en}?", {"x": SHOPPING_ITEMS}, "B1", "Desconto para"),
    ],
    "transportation": [
        ("where", "Onde pego {x.pt}?", "Where do I catch {x.en}?", {"x": TRANSPORTS}, "A1", "Onde pego...?"),
        ("destination", "Quero ir para {x.pt}.", "I want to go to {x.en}.", {"x": DESTINATIONS}, "A1", "Ir para + place"),
        ("ticket", "Preciso de uma passagem para {x.pt}.", "I need a ticket to {x.en}.", {"x": DESTINATIONS}, "A2", "Passagem para"),
        ("arrival", "{x.pt} chega que horas?", "What time does {x.en} arrive?", {"x": TRANSPORTS}, "A2", "Chegar + time"),
        ("leaving", "{x.pt} sai que horas?", "What time does {x.en} leave?", {"x": TRANSPORTS}, "A2", "Sair + time"),
        ("route", "Esse caminho vai para {x.pt}?", "Does this route go to {x.en}?", {"x": DESTINATIONS}, "A2", "Vai para + place"),
        ("late", "{x.pt} está atrasado?", "Is {x.en} late?", {"x": TRANSPORTS}, "A2", "Estar atrasado"),
        ("platform", "Qual é a plataforma para {x.pt}?", "Which platform is for {x.en}?", {"x": DESTINATIONS}, "A2", "Qual é + noun"),
        ("stop", "Preciso descer em {x.pt}.", "I need to get off at {x.en}.", {"x": DESTINATIONS}, "A2", "Descer em + place"),
        ("connection", "Tenho conexão para {x.pt}.", "I have a connection to {x.en}.", {"x": DESTINATIONS}, "B1", "Conexão para"),
        ("time_to", "Quanto tempo até {x.pt}?", "How long until {x.en}?", {"x": DESTINATIONS}, "A2", "Quanto tempo até...?"),
    ],
    "emotions": [
        ("state", "Estou {x.pt}.", "I am {x.en}.", {"x": EMOTIONS}, "A1", "Estar + adjective"),
        ("feel", "Me sinto {x.pt}.", "I feel {x.en}.", {"x": EMOTIONS[:12]}, "A2", "Sentir-se"),
        ("because", "Estou {x.pt} porque {y.pt}.", "I am {x.en} because {y.en}.", {"x": EMOTIONS[:10], "y": [slot("estou aprendendo", "I am learning"), slot("estou atrasado", "I am late"), slot("estou viajando", "I am traveling"), slot("não entendi", "I did not understand"), slot("deu certo", "it worked")]}, "B1", "Porque + clause"),
        ("reaction", "Isso me deixa {x.pt}.", "That makes me {x.en}.", {"x": EMOTIONS[:10]}, "B1", "Deixar + adjective"),
    ],
    "questions": [
        ("where_can", "Onde posso {x.pt}?", "Where can I {x.en}?", {"x": QUESTION_ACTIONS}, "A1", "Onde posso + infinitive"),
        ("how_do", "Como faço para {x.pt}?", "How do I {x.en}?", {"x": QUESTION_ACTIONS}, "A2", "Como faço para + infinitive"),
        ("can_you", "Você pode me ajudar a {x.pt}?", "Can you help me {x.en}?", {"x": QUESTION_ACTIONS}, "A2", "Ajudar a + infinitive"),
        ("do_you_know", "Você sabe se posso {x.pt}?", "Do you know if I can {x.en}?", {"x": QUESTION_ACTIONS}, "B1", "Você sabe se...?"),
        ("what_time", "Que horas posso {x.pt}?", "What time can I {x.en}?", {"x": QUESTION_ACTIONS}, "A2", "Que horas...?"),
        ("where_is", "Onde fica {x.pt}?", "Where is {x.en}?", {"x": PLACES}, "A1", "Onde fica...?"),
        ("how_much", "Quanto custa ir para {x.pt}?", "How much does it cost to go to {x.en}?", {"x": DESTINATIONS}, "A2", "Quanto custa...?"),
        ("when_can", "Quando posso {x.pt}?", "When can I {x.en}?", {"x": QUESTION_ACTIONS}, "A2", "Quando posso...?"),
        ("which_is", "Qual é o melhor jeito de {x.pt}?", "What is the best way to {x.en}?", {"x": QUESTION_ACTIONS}, "B1", "Qual é o melhor jeito...?"),
    ],
    "verbs": [
        ("want", "Eu quero {x.pt}.", "I want to {x.en}.", {"x": VERB_ACTIONS}, "A1", "Querer + infinitive"),
        ("need", "Eu preciso {x.pt}.", "I need to {x.en}.", {"x": VERB_ACTIONS}, "A1", "Precisar + infinitive"),
        ("can", "Eu posso {x.pt}.", "I can {x.en}.", {"x": VERB_ACTIONS}, "A1", "Poder + infinitive"),
        ("will", "Eu vou {x.pt}.", "I am going to {x.en}.", {"x": VERB_ACTIONS}, "A1", "Ir + infinitive"),
        ("like", "Eu gosto de {x.pt}.", "I like to {x.en}.", {"x": VERB_ACTIONS}, "A2", "Gostar de + infinitive"),
        ("try", "Vou tentar {x.pt}.", "I will try to {x.en}.", {"x": VERB_ACTIONS}, "A2", "Tentar + infinitive"),
        ("learn", "Estou aprendendo a {x.pt}.", "I am learning to {x.en}.", {"x": VERB_ACTIONS[:10]}, "A2", "Aprender a + infinitive"),
    ],
    "sentence_patterns": [
        ("would_like", "Eu gostaria de {x.pt}.", "I would like {x.en}.", {"x": NEEDS + FOODS[:10]}, "A1", "Eu gostaria de + noun"),
        ("could_you", "Você poderia {x.pt}?", "Could you {x.en}?", {"x": QUESTION_ACTIONS + VERB_ACTIONS[:10]}, "A2", "Poderia + infinitive"),
        ("it_is_better", "É melhor {x.pt}.", "It is better to {x.en}.", {"x": VERB_ACTIONS}, "B1", "É melhor + infinitive"),
        ("if_time", "Se eu tiver tempo, vou {x.pt}.", "If I have time, I will {x.en}.", {"x": VERB_ACTIONS}, "B1", "Se + future subjunctive"),
        ("before", "Antes de {x.pt}, preciso confirmar.", "Before I {x.en}, I need to confirm.", {"x": VERB_ACTIONS[:12]}, "B1", "Antes de + infinitive"),
        ("after", "Depois de {x.pt}, eu volto.", "After I {x.en}, I will come back.", {"x": VERB_ACTIONS[:12]}, "B1", "Depois de + infinitive"),
        ("when", "Quando eu {x.pt}, aviso você.", "When I {x.en}, I will let you know.", {"x": VERB_ACTIONS[:16]}, "B1", "Quando + future"),
        ("because", "Não posso {x.pt} porque estou ocupado.", "I cannot {x.en} because I am busy.", {"x": VERB_ACTIONS[:16]}, "B1", "Porque + reason"),
    ],
}


def generate() -> list[dict]:
    phrases: list[dict] = []
    seen_pt: set[str] = set()
    seen_ids: set[str] = set()

    for category, target in TARGETS.items():
        category_items: list[dict] = []
        frames = FRAMESETS[category]
        frame_round = 0
        while len(category_items) < target:
            made_progress = False
            for frame in frames:
                subcategory, pt_template, en_template, slots, difficulty, note = frame
                for combo in expand_slots(slots):
                    pt = render(pt_template, combo)
                    if pt in seen_pt:
                        continue
                    en = render(en_template, combo)
                    tags = [category, subcategory] + [value["tag"] for value in combo.values()]
                    phrase_id = unique_id(
                        f"{category}-{subcategory}-{slug(pt)}",
                        seen_ids,
                    )
                    item = {
                        "id": phrase_id,
                        "portuguese": pt,
                        "english": en,
                        "pronunciationGuide": pronunciation_guide(pt),
                        "category": category,
                        "subcategory": subcategory,
                        "difficulty": difficulty,
                        "tags": sorted(set(tags)),
                        "speakingPractice": True,
                        "grammarNote": note,
                    }
                    phrases.append(item)
                    category_items.append(item)
                    seen_pt.add(pt)
                    made_progress = True
                    if len(category_items) >= target:
                        break
                if len(category_items) >= target:
                    break
            frame_round += 1
            if not made_progress or frame_round > 3:
                break
        if len(category_items) != target:
            raise RuntimeError(f"{category}: generated {len(category_items)}, expected {target}")

    return phrases


def expand_slots(slots: dict[str, list[dict[str, str]]]) -> list[dict[str, dict[str, str]]]:
    if not slots:
        return [{}]
    keys = list(slots)
    return [dict(zip(keys, values)) for values in product(*(slots[key] for key in keys))]


def render(template: str, combo: dict[str, dict[str, str]]) -> str:
    rendered = template
    for key, value in combo.items():
        rendered = rendered.replace("{" + key + ".pt}", value["pt"])
        rendered = rendered.replace("{" + key + ".en}", value["en"])
    return rendered


def pronunciation_guide(text: str) -> str:
    rough = text.lower()
    replacements = [
        ("ções", "SOYNS"), ("ção", "SOWN"), ("ões", "OYNS"), ("ão", "OWN"),
        ("ães", "INES"), ("ãe", "INE"), ("nh", "ny"), ("lh", "ly"), ("ch", "sh"),
        ("gue", "geh"), ("gui", "gee"), ("que", "keh"), ("qui", "kee"),
        ("j", "zh"), ("x", "sh"), ("ç", "s"),
    ]
    for src, dst in replacements:
        rough = rough.replace(src, dst)
    rough = unicodedata.normalize("NFD", rough)
    rough = "".join(ch for ch in rough if unicodedata.category(ch) != "Mn")
    rough = re.sub(r"\s+", " ", rough).strip()
    return f"pt-BR: {rough}"


def slug(value: str) -> str:
    raw = unicodedata.normalize("NFD", value.lower())
    raw = "".join(ch for ch in raw if unicodedata.category(ch) != "Mn")
    raw = re.sub(r"[^a-z0-9]+", "-", raw).strip("-")
    return raw[:80] or "phrase"


def unique_id(base: str, seen: set[str]) -> str:
    candidate = base
    suffix = 2
    while candidate in seen:
        candidate = f"{base}-{suffix}"
        suffix += 1
    seen.add(candidate)
    return candidate


def validate(phrases: list[dict]) -> None:
    ids = [item["id"] for item in phrases]
    if len(ids) != len(set(ids)):
        raise RuntimeError("Duplicate ids generated")
    required = ["id", "portuguese", "english", "pronunciationGuide", "category"]
    for item in phrases:
        for key in required:
            if not str(item.get(key, "")).strip():
                raise RuntimeError(f"{item.get('id', '<missing>')} missing {key}")


def main() -> None:
    phrases = generate()
    validate(phrases)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(phrases, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(phrases)} phrases to {OUT}")
    for category in TARGETS:
        count = sum(1 for item in phrases if item["category"] == category)
        print(f"{category}: {count}")


if __name__ == "__main__":
    main()
