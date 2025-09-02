import { useRef, useState } from "react";
import { motion } from "motion/react";

const letters = ["L", "P", "A", "P"];
const fullWords = ["Lucas", "Pango's", "Authentication", "Platform"];

const LPAP = () => {
  const [hovered, setHovered] = useState(false);
  const hoverRef = useRef(false);
  const [expandWords, setExpandWords] = useState(false);

  // Trigger expand after stacking animation completes
  const handleMouseEnter = () => {
    setHovered(true);
    hoverRef.current = true;
    setExpandWords(false);
    setTimeout(() => {
      if (hoverRef.current) setExpandWords(true);
    }, 600); // wait for stack animation
  };

  const handleMouseLeave = () => {
    setHovered(false);
    hoverRef.current = false;
    setExpandWords(false);
  };

  return (
    <div
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      className="flex items-center justify-center"
    >
      <motion.div
        layout
        initial={false}
        animate={{
          height: hovered ? "130px" : "32px",
          width: hovered ? "50%" : "20%",
        }}
        className="relative m-3"
      >
        {letters.map((char, i) => (
          <motion.div
            key={i}
            layout
            initial={false}
            animate={{
              position: "absolute",
              top: hovered ? i * 30 : 0,
              left: hovered ? 0 : i * 20,
            }}
            transition={{
              type: "spring",
              stiffness: 300,
              damping: 25,
              delay: i * 0.05,
            }}
            className="text-3xl font-bold"
          >
            <motion.span
              style={{ fontFamily: "" }}
              key={expandWords ? `word-${i}` : `letter-${i}`}
              initial={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              transition={{ delay: 0.1, duration: 0.3 }}
            >
              {expandWords ? fullWords[i] : char}
            </motion.span>
          </motion.div>
        ))}
        <hr className="absolute bottom-0 left-0 w-full border-t border-black" />
      </motion.div>
    </div>
  );
};

export default LPAP;
