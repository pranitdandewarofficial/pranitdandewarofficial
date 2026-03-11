const examSelect = document.getElementById("exam");
const streamControl = document.getElementById("stream-control");
const streamSelect = document.getElementById("stream");
const difficultyInput = document.getElementById("difficulty");
const difficultyOutput = document.getElementById("difficulty-output");
const questionCountSelect = document.getElementById("question-count");
const generateButton = document.getElementById("generate");
const exportButton = document.getElementById("export");

const paperTitle = document.getElementById("paper-title");
const paperSubtitle = document.getElementById("paper-subtitle");
const paperTags = document.getElementById("paper-tags");
const paperRules = document.getElementById("paper-rules");
const paperBody = document.getElementById("paper-body");

const difficultyLabels = [
  "Easy (Boards/CET level)",
  "Exam-Level simulation",
  "Slightly Tough (JEE Main +)",
  "Advanced only",
];

const topicBank = {
  physics: [
    "Kinematics",
    "Laws of Motion",
    "Work, Energy & Power",
    "Electrostatics",
    "Current Electricity",
    "Magnetism",
    "Modern Physics",
    "Ray Optics",
    "Waves",
    "Thermodynamics",
  ],
  chemistry: [
    "Atomic Structure",
    "Chemical Bonding",
    "Thermodynamics",
    "Electrochemistry",
    "Solutions",
    "Organic Reactions",
    "Hydrocarbons",
    "Coordination Compounds",
    "Chemical Kinetics",
    "p-Block Elements",
  ],
  maths: [
    "Quadratic Equations",
    "Trigonometry",
    "Vectors",
    "Coordinate Geometry",
    "Differentiation",
    "Integration",
    "Probability",
    "Complex Numbers",
    "Matrices",
    "Sequences & Series",
  ],
  biology: [
    "Genetics",
    "Human Physiology",
    "Plant Physiology",
    "Cell Biology",
    "Ecology",
    "Biomolecules",
    "Evolution",
    "Biotechnology",
    "Reproduction",
    "Microbes",
  ],
};

const examBlueprints = {
  cet: {
    name: "MHT-CET",
    time: "180 minutes",
    focus: "Speed + repetition + formulas",
    sections: (stream) =>
      stream === "pcb"
        ? [
            { subject: "Physics", questions: 50, marksPer: 1, type: "MCQ" },
            { subject: "Chemistry", questions: 50, marksPer: 1, type: "MCQ" },
            { subject: "Biology", questions: 100, marksPer: 1, type: "MCQ" },
          ]
        : [
            { subject: "Physics", questions: 50, marksPer: 1, type: "MCQ" },
            { subject: "Chemistry", questions: 50, marksPer: 1, type: "MCQ" },
            { subject: "Maths", questions: 50, marksPer: 2, type: "MCQ" },
          ],
    rules: [
      "MCQ only",
      "No negative marking",
      "Maths questions = 2 marks each",
      "Physics/Chemistry/Biology = 1 mark each",
      "Easy → Moderate difficulty",
    ],
  },
  boards: {
    name: "Maharashtra HSC Boards (12th)",
    time: "3 hours per paper",
    focus: "Answer-writing + textbook language",
    sections: () => [
      { subject: "Physics", questions: 18, marksPer: 4, type: "Theory + Numericals" },
      { subject: "Chemistry", questions: 18, marksPer: 4, type: "Theory + Numericals" },
      { subject: "Maths", questions: 20, marksPer: 4, type: "Theory + Numericals" },
    ],
    rules: [
      "Step-wise marking",
      "Definition + derivation heavy",
      "Standard numericals from textbook",
      "Language & presentation matter",
      "Easy → Moderate difficulty",
    ],
  },
  "jee-main": {
    name: "JEE Main",
    time: "180 minutes",
    focus: "Accuracy + standard concepts",
    sections: () => [
      { subject: "Physics", questions: 25, marksPer: 4, type: "MCQ + Numerical" },
      { subject: "Chemistry", questions: 25, marksPer: 4, type: "MCQ + Numerical" },
      { subject: "Maths", questions: 25, marksPer: 4, type: "MCQ + Numerical" },
    ],
    rules: [
      "20 MCQ + 5 numerical per subject",
      "MCQ: −1 negative marking",
      "Numericals: no negative",
      "Balanced difficulty, not too easy, not too tough",
    ],
  },
  "jee-advanced": {
    name: "JEE Advanced",
    time: "Paper 1 + Paper 2",
    focus: "Depth + integration + thinking",
    sections: () => [
      { subject: "Physics", questions: 18, marksPer: 3, type: "Mixed types" },
      { subject: "Chemistry", questions: 18, marksPer: 3, type: "Mixed types" },
      { subject: "Maths", questions: 18, marksPer: 3, type: "Mixed types" },
    ],
    rules: [
      "Two papers with changing patterns",
      "MCQ (single/multi), integer, match, paragraph",
      "Hard → Very Hard",
      "Multi-concept integration",
    ],
  },
};

const questionTypeMap = {
  boards: ["Definition", "Derivation", "Standard numerical", "Theory explanation"],
  cet: ["MCQ"],
  "jee-main": ["MCQ", "Numerical"],
  "jee-advanced": ["Single correct", "Multiple correct", "Integer", "Match", "Paragraph"],
};

const focusMap = {
  boards: "Answer-writing optimized, not MCQ-heavy",
  cet: "CET mocks should feel easy but time-pressured",
  "jee-main": "Balanced, exam-level simulation",
  "jee-advanced": "Uncomfortable but fair, depth-first",
};

const difficultyFocus = {
  1: "Easy – Board/CET aligned, direct concepts",
  2: "Exam-Level – actual exam simulation",
  3: "Slightly Tough – deeper standard concepts",
  4: "Advanced – multi-concept integration",
};

const getRandomItem = (arr) => arr[Math.floor(Math.random() * arr.length)];

const clampCount = (count) => {
  if (questionCountSelect.value === "compact") {
    return Math.max(8, Math.ceil(count * 0.5));
  }
  return count;
};

const buildRulesList = (rules) => {
  const list = document.createElement("ul");
  rules.forEach((rule) => {
    const li = document.createElement("li");
    li.textContent = rule;
    list.appendChild(li);
  });
  return list;
};

const buildQuestion = (index, subject, examKey, type, marksPer) => {
  const topics = topicBank[subject.toLowerCase()] || topicBank.physics;
  const topic = getRandomItem(topics);
  const questionType = type || getRandomItem(questionTypeMap[examKey]);
  const question = document.createElement("div");
  question.className = "question";

  const meta = document.createElement("div");
  meta.className = "meta";
  meta.innerHTML = `<span>Q${index} • ${questionType}</span><span>${marksPer} marks</span>`;

  const text = document.createElement("p");
  text.textContent = `From ${topic}, craft a ${questionType.toLowerCase()} focusing on ${examBlueprints[examKey].focus}.`;

  question.appendChild(meta);
  question.appendChild(text);

  if (examKey === "boards") {
    const answerSpace = document.createElement("div");
    answerSpace.className = "answer-space";
    answerSpace.textContent = "Answer space: include definitions, derivations, and steps.";
    question.appendChild(answerSpace);
  }

  return question;
};

const buildSection = (section, examKey) => {
  const wrapper = document.createElement("div");
  wrapper.className = "section";

  const heading = document.createElement("h3");
  heading.textContent = `${section.subject} (${section.type})`;

  const subtitle = document.createElement("p");
  subtitle.className = "muted";
  subtitle.textContent = `Questions: ${section.questions} • ${section.marksPer} marks each`;

  const list = document.createElement("div");
  list.className = "question-list";

  const questionTotal = clampCount(section.questions);

  for (let i = 1; i <= questionTotal; i += 1) {
    list.appendChild(buildQuestion(i, section.subject, examKey, section.type, section.marksPer));
  }

  wrapper.appendChild(heading);
  wrapper.appendChild(subtitle);
  wrapper.appendChild(list);

  return wrapper;
};

const renderPaper = () => {
  const examKey = examSelect.value;
  const stream = streamSelect.value;
  const blueprint = examBlueprints[examKey];
  const difficultyValue = Number(difficultyInput.value);

  paperTitle.textContent = `${blueprint.name} Mock Test`;
  paperSubtitle.textContent = `Focus: ${blueprint.focus}`;

  paperTags.innerHTML = "";
  const tags = [
    `Time: ${blueprint.time}`,
    `Difficulty: ${difficultyLabels[difficultyValue - 1]}`,
    focusMap[examKey],
  ];
  tags.forEach((tag) => {
    const pill = document.createElement("span");
    pill.className = "pill";
    pill.textContent = tag;
    paperTags.appendChild(pill);
  });

  paperRules.innerHTML = "";
  const rulesTitle = document.createElement("h3");
  rulesTitle.textContent = "Exam rules";
  const rules = buildRulesList(blueprint.rules);
  paperRules.appendChild(rulesTitle);
  paperRules.appendChild(rules);

  const note = document.createElement("p");
  note.className = "muted";
  note.textContent = `App intelligence: ${blueprint.focus}. Difficulty mode: ${difficultyFocus[difficultyValue]}.`;
  paperRules.appendChild(note);

  paperBody.innerHTML = "";
  blueprint.sections(stream).forEach((section) => {
    paperBody.appendChild(buildSection(section, examKey));
  });
};

const updateDifficultyLabel = () => {
  difficultyOutput.textContent = difficultyLabels[difficultyInput.value - 1];
};

const handleExamChange = () => {
  streamControl.style.display = examSelect.value === "cet" ? "block" : "none";
};

updateDifficultyLabel();
handleExamChange();

examSelect.addEventListener("change", handleExamChange);
difficultyInput.addEventListener("input", updateDifficultyLabel);

generateButton.addEventListener("click", () => {
  renderPaper();
  paperSubtitle.scrollIntoView({ behavior: "smooth", block: "center" });
});

exportButton.addEventListener("click", () => {
  if (!paperBody.children.length) {
    renderPaper();
  }
  window.print();
});
