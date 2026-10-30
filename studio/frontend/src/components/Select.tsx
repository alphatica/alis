import { For } from 'solid-js';

/** Drop-down field with a label, like Field. */
export function Select(props: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: string[];
  disabled?: boolean;
}) {
  return (
    <div class="field">
      <label for={props.id}>{props.label}</label>
      <select id={props.id} disabled={props.disabled} value={props.value} onChange={(event) => props.onChange(event.currentTarget.value)}>
        <For each={props.options}>{(name) => <option value={name}>{name}</option>}</For>
      </select>
    </div>
  );
}
