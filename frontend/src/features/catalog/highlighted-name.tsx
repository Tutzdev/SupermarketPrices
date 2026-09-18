function normalize(text: string) {
  return text.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
}

export function HighlightedName({ name, query }: { name: string; query: string }) {
  const terms = normalize(query).split(/[^a-z0-9]+/).filter((term) => term.length >= 2);
  return name.split(/(\s+|-)/).map((part, index) => terms.some((term) => normalize(part).includes(term))
    ? <mark key={index} className="rounded-sm bg-primary-soft text-inherit">{part}</mark> : part);
}
