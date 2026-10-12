import type { JSX } from 'solid-js';

/** Text field with its label above, for forms in .form-stack. */
export function Field(props: {
  id: string;
  label: string;
  value: string;
  onInput: (value: string) => void;
  /** On Enter or leaving the field after a change. */
  onChange?: (value: string) => void;
  placeholder?: string;
  title?: string;
  disabled?: boolean;
  inputmode?: JSX.HTMLAttributes<HTMLInputElement>['inputmode'];
}) {
  return (
    <div class="field">
      <label for={props.id}>{props.label}</label>
      <input
        id={props.id}
        type="text"
        inputmode={props.inputmode}
        placeholder={props.placeholder}
        title={props.title}
        disabled={props.disabled}
        value={props.value}
        onInput={(event) => props.onInput(event.currentTarget.value)}
        onChange={(event) => props.onChange?.(event.currentTarget.value)}
      />
    </div>
  );
}
