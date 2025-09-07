import tkinter as tk
from dataclasses import dataclass
try:
    from plyer import notification
except Exception:  # plyer may not be available in all environments
    notification = None


@dataclass
class PokemonEncounter:
    """Represent a single Pokémon encounter."""
    name: str
    is_shiny: bool = False
    is_legendary: bool = False


class EncounterCounterApp:
    """UI application that tracks Pokémon encounters.

    Features
    --------
    * Tray tips for every encounter
    * Notifications for shiny and legendary encounters
    * Option to automatically reset count when a shiny is found
    """

    def __init__(self) -> None:
        self.root = tk.Tk()
        self.root.title("PokeMMO Encounter Counter")

        self.encounters = 0
        self.latest_shiny = "None"
        self.auto_reset_var = tk.BooleanVar(value=False)

        self.enc_label = tk.Label(self.root, text=f"Encounters: {self.encounters}")
        self.enc_label.pack(pady=5)

        self.shiny_label = tk.Label(
            self.root, text=f"Latest shiny encounter: {self.latest_shiny}"
        )
        self.shiny_label.pack(pady=5)

        self.auto_reset_cb = tk.Checkbutton(
            self.root, text="Auto reset on shiny", variable=self.auto_reset_var
        )
        self.auto_reset_cb.pack(pady=5)

    def notify(self, title: str, message: str, timeout: int = 3) -> None:
        """Send a system tray notification if possible."""
        if notification is None:
            return
        try:
            notification.notify(title=title, message=message, timeout=timeout)
        except Exception:
            pass  # Notification failures are non-critical

    def encounter(self, encounter: PokemonEncounter) -> None:
        """Record an encounter and update the UI/notifications."""
        self.encounters += 1
        self.enc_label.config(text=f"Encounters: {self.encounters}")

        # Tray tip for every encounter
        self.notify("Pokémon Encountered", encounter.name)

        if encounter.is_shiny:
            self.notify("Shiny Encounter!", encounter.name, timeout=5)
            self.latest_shiny = encounter.name
            self.shiny_label.config(
                text=f"Latest shiny encounter: {self.latest_shiny}"
            )
            if self.auto_reset_var.get():
                self.encounters = 0
                self.enc_label.config(text=f"Encounters: {self.encounters}")
        if encounter.is_legendary:
            self.notify("Legendary Encounter!", encounter.name, timeout=5)

    def run(self) -> None:
        self.root.mainloop()


if __name__ == "__main__":
    app = EncounterCounterApp()

    # Demo button to simulate encounters for manual testing
    def simulate():
        app.encounter(PokemonEncounter("Pikachu"))

    tk.Button(app.root, text="Simulate Encounter", command=simulate).pack(pady=10)
    app.run()
