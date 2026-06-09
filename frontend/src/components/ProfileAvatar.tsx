interface ProfileAvatarProps {
  name: string;
  photo?: string | null;
  size?: "sm" | "md" | "lg";
}

export function ProfileAvatar({ name, photo, size = "md" }: ProfileAvatarProps) {
  const initials = name
    .split(" ")
    .map((word) => word[0])
    .join("")
    .slice(0, 2)
    .toUpperCase() || "PH";

  return (
    <div className={`avatar profile-avatar ${size}`}>
      {photo ? <img src={photo} alt={`${name} profile`} /> : initials}
    </div>
  );
}
